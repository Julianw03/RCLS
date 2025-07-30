package com.julianw03.rcls.service.base.cacheService.rcu.impl.publishing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.rcu.RCUStateService;
import com.julianw03.rcls.service.base.cacheService.PublishingObjectDataManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PublishingRsoAuthenticationManager extends PublishingObjectDataManager<RsoAuthenticatorV1AuthenticationResponse> {
    private static final Pattern RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN = Pattern.compile("^/rso-authenticator/v1/authentication$");

    protected PublishingRsoAuthenticationManager(RiotClientService riotClientService, RCUStateService cacheService) {
        super(riotClientService, cacheService);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    @Override
    public void setState(RsoAuthenticatorV1AuthenticationResponse state) {
        Optional.ofNullable(state).map(RsoAuthenticatorV1AuthenticationResponse::getSuccess).ifPresent(success -> {
            success.setLoginToken(null);
            success.setRedirectUrl(null);
        });

        super.setState(state);
    }

    @Override
    protected CompletableFuture<RsoAuthenticatorV1AuthenticationResponse> doFetchInitialData() {
        Optional<CoreSdkApi> optionalCoreSdkApi = riotClientService.getApi(CoreSdkApi.class);

        if (optionalCoreSdkApi.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to build CoreSdkApi client for RsoAuthSessionManager")
            );
        }

        CoreSdkApi coreSdkApi = optionalCoreSdkApi.get();

        final RsoAuthenticatorV1AuthenticationResponse resp;
        try {
            resp = coreSdkApi.rsoAuthenticatorV1AuthenticationGet();
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
        return RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN .matcher(uri);
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

                setState(authenticationResponse);
            }
        }
    }
}
