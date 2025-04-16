package com.julianw03.rcls.service.cacheService.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.api.RsoAuthSessionResponse;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.cacheService.ObjectDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RsoAuthSessionManager extends ObjectDataManager<RsoAuthSessionResponse> {
    private static final Pattern LOL_RSO_AUTH_SESSION_PATTERN = Pattern.compile("^/rso-auth/v1/session$");

    public RsoAuthSessionManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }

    @Override
    protected CompletableFuture<RsoAuthSessionResponse> doFetchInitialData() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.GET,
                "/rso-auth/v1/session",
                null
        );

        if (Objects.requireNonNull(response) instanceof InternalApiResponse.Success successResponse) {
            Optional<RsoAuthSessionResponse> opt = successResponse.map(
                    objectMapper,
                    new TypeReference<>() {}
            );
            if (opt.isPresent()) {
                return CompletableFuture.completedFuture(opt.get());
            }
        }
        return CompletableFuture.failedFuture(
                new Exception("Failed to fetch initial data for RsoAuthenticationManager")
        );
    }

    @Override
    protected Matcher getUriMatcher(String uri) {
        return LOL_RSO_AUTH_SESSION_PATTERN.matcher(uri);
    }

    @Override
    protected void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher) {
        switch (type) {
            case DELETE -> resetInternalState();
            case CREATE, UPDATE -> {
                Optional<RsoAuthSessionResponse> updatedData = parseJson(data, RsoAuthSessionResponse.class);
                if (updatedData.isEmpty()) {
                    log.warn("Successfully matched pattern, but unable to parse JSON: {}", data);
                    return;
                }
                RsoAuthSessionResponse authenticationResponse = updatedData.get();
                log.info("Updating RsoAuthSessionManager with data: {}", authenticationResponse);
                setState(authenticationResponse);
            }
        }
    }
}
