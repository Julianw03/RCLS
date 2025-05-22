package com.julianw03.rcls.service.base.cacheService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.RsoAuthSessionResponse;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.ObjectDataManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        Optional<CoreSdkApi> optionalCoreSdkApi = riotClientService.getApiClient().map(
                client -> client.buildClient(CoreSdkApi.class)
        );

        if (optionalCoreSdkApi.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to build CoreSdkApi client for RsoAuthSessionManager")
            );
        }

        CoreSdkApi coreSdkApi = optionalCoreSdkApi.get();

        final RsoAuthSessionResponse resp;
        try {
            resp = coreSdkApi.rsoAuthV1SessionGet();
        } catch (Exception e) {
            log.error("Failed to fetch initial data for RsoAuthSessionManager", e);
            return CompletableFuture.failedFuture(e);
        }

        if (resp == null) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to fetch initial data for RsoAuthSessionManager")
            );
        }

        return CompletableFuture.completedFuture(resp);
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
