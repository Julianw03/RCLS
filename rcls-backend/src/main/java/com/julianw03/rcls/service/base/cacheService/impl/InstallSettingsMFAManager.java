package com.julianw03.rcls.service.base.cacheService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.ObjectDataManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
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
public class InstallSettingsMFAManager extends ObjectDataManager<Boolean> {
    private final Pattern lolInstallSettingsMFA = Pattern.compile("^/data-store/v1/install-settings/mfa_notification_dismissed$");

    protected InstallSettingsMFAManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }


    @Override
    protected CompletableFuture<Boolean> doFetchInitialData() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.GET,
                "/data-store/v1/install-settings/mfa_notification_dismissed",
                null
        );

        if (Objects.requireNonNull(response) instanceof InternalApiResponse.Success successResponse) {
            Optional<Boolean> opt = successResponse.map(
                    objectMapper,
                    Boolean.class
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
        return lolInstallSettingsMFA.matcher(uri);
    }

    @Override
    protected void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher) {

        switch (type) {
            case UPDATE, CREATE -> {
                Optional<Boolean> optParsed = parseJson(data, Boolean.class);

                if (optParsed.isEmpty()) {
                    log.warn("Pattern did match, but data was not a boolean: {}", data);
                    return;
                }

                final Boolean parsed = optParsed.get();
                log.warn("Parsed data: {}", parsed);

                log.warn("Setting state to {}", parsed);
                setState(parsed);
            }
            case DELETE -> {
                log.warn("Deleting state");
                resetInternalState();
            }
        }
    }
}
