package com.julianw03.rcls.service.cacheService.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.api.ProductSession;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.cacheService.MapDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SessionsManager extends MapDataManager<String, ProductSession> {
    private static final Pattern lolProductSessionPattern = Pattern.compile("^/product-session/v1/sessions/([\\w-_]{0,50})$");

    public SessionsManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }

    @Override
    protected CompletableFuture<Map<String, ProductSession>> doFetchInitialData() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.GET,
                "/product-session/v1/sessions",
                null
        );

        if (Objects.requireNonNull(response) instanceof InternalApiResponse.Success successResponse) {
            Optional<Map<String, ProductSession>> opt = successResponse.map(
                    objectMapper,
                    new TypeReference<Map<String, ProductSession>>() {}
            );
            if (opt.isPresent()) {
                return CompletableFuture.completedFuture(opt.get());
            }
        }
        return CompletableFuture.failedFuture(
                new Exception("Failed to fetch initial data for InstallSettingsMFAManager")
        );
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
                Optional<ProductSession> updatedSession = parseJson(data, ProductSession.class);
                if (updatedSession.isEmpty()) {
                    log.warn("Successfully matched pattern, but unable to parse JSON: {}", data);
                    return;
                }
                ProductSession session = updatedSession.get();
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
