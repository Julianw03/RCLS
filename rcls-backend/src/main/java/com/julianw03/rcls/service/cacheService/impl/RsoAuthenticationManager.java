package com.julianw03.rcls.service.cacheService.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.api.ProductSession;
import com.julianw03.rcls.model.api.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.cacheService.DataManager;
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

@Slf4j
@Component
public class RsoAuthenticationManager extends ObjectDataManager<RsoAuthenticatorV1AuthenticationResponse> {
    private static final Pattern RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN = Pattern.compile("^/rso-authenticator/v1/authentication$");

    public RsoAuthenticationManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }


    @Override
    protected CompletableFuture<RsoAuthenticatorV1AuthenticationResponse> doFetchInitialData() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.GET,
                "/rso-authenticator/v1/authentication",
                null
        );

        if (Objects.requireNonNull(response) instanceof InternalApiResponse.Success successResponse) {
            Optional<RsoAuthenticatorV1AuthenticationResponse> opt = successResponse.map(
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
        return RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN.matcher(uri);
    }

    @Override
    protected void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher) {

        switch (type) {
            case DELETE -> resetInternalState();
            case CREATE, UPDATE -> {
                Optional<RsoAuthenticatorV1AuthenticationResponse> updatedData = parseJson(data, RsoAuthenticatorV1AuthenticationResponse.class);
                if (updatedData.isEmpty()) {
                    log.warn("Successfully matched pattern, but unable to parse JSON: {}", data);
                    return;
                }
                RsoAuthenticatorV1AuthenticationResponse authenticationResponse = updatedData.get();
                log.info("Updating RsoAuthenticationManager with data: {}", authenticationResponse);
                setState(authenticationResponse);
            }
        }
    }
}
