package com.julianw03.rcls.service.base.cacheService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.ProductSession;
import com.julianw03.rcls.generated.model.ProductSessionSession;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.MapDataManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SessionsManager extends MapDataManager<String, ProductSessionSession> {
    private static final Pattern lolProductSessionPattern = Pattern.compile("^/product-session/v1/sessions/([\\w-_]{0,50})$");

    public SessionsManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }

    @Override
    protected CompletableFuture<Map<String, ProductSessionSession>> doFetchInitialData() {
        Optional<CoreSdkApi> optionalCoreSdkApi = riotClientService.getApiClient().map(
                client -> client.buildClient(CoreSdkApi.class)
        );

        if (optionalCoreSdkApi.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to build CoreSdkApi client for RsoAuthSessionManager")
            );
        }

        CoreSdkApi coreSdkApi = optionalCoreSdkApi.get();

        final Map<String, ProductSessionSession> resp;
        try {
            resp = coreSdkApi.productSessionV1SessionsGet();
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
        return lolProductSessionPattern.matcher(uri);
    }

    @Override
    protected void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher) {

        final String sessionId = uriMatcher.group(1);

        switch (type) {
            case CREATE, UPDATE -> {
                Optional<ProductSessionSession> updatedSession = parseJson(data, ProductSessionSession.class);
                if (updatedSession.isEmpty()) {
                    log.warn("Successfully matched pattern, but unable to parse JSON: {}", data);
                    return;
                }
                ProductSessionSession session = updatedSession.get();
                log.warn("Add Session {}: {}", sessionId, session);
                map.put(sessionId, session);
            }
            case DELETE -> {
                log.warn("Remove Session {}", sessionId);
                map.remove(sessionId);
            }
        }
    }
}
