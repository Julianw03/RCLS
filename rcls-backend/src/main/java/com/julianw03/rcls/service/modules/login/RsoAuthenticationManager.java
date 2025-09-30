package com.julianw03.rcls.service.modules.login;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.*;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.data.ObjectDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.modules.login.model.AuthenticationStateDTO;
import com.julianw03.rcls.service.modules.login.model.HCaptchaDTO;
import com.julianw03.rcls.service.modules.login.model.LoginStatusDTO;
import com.julianw03.rcls.service.modules.login.model.MultifactorInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RsoAuthenticationManager extends ObjectDataManager<RsoAuthenticatorV1AuthenticationResponse, AuthenticationStateDTO> {
    private static final Pattern RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN = Pattern.compile("^/rso-authenticator/v1/authentication$");

    public RsoAuthenticationManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );
    }


    @Override
    protected AuthenticationStateDTO mapView(RsoAuthenticatorV1AuthenticationResponse state) {
        if (state == null) {
            return AuthenticationStateDTO.builder()
                                         .loginStatus(LoginStatusDTO.UNKNOWN)
                                         .build();
        }

        final HCaptchaDTO hCaptchaDTO = Optional.ofNullable(state.getCaptcha())
                                                .map(RsoAuthenticatorV1Captcha::getHcaptcha)
                                                .map(RsoAuthenticationManager::hCaptchaFromInternal)
                                                .orElse(null);

        final MultifactorInfoDTO multifactorInfoDTO = Optional.ofNullable(state.getMultifactor())
                                                              .map(RsoAuthenticationManager::multifactorInfoFromInternal)
                                                              .orElse(null);

        final LoginStatusDTO status = Optional.ofNullable(state.getType())
                                              .map(RsoAuthenticationManager::loginStatusFromInternal)
                                              .orElse(LoginStatusDTO.UNKNOWN);

        return AuthenticationStateDTO.builder()
                                     .loginStatus(status)
                                     .multifactorInfo(multifactorInfoDTO)
                                     .hcaptchaStatus(hCaptchaDTO)
                                     .countryCode(state.getCountry())
                                     .build();
    }

    private static MultifactorInfoDTO multifactorInfoFromInternal(RsoAuthenticatorV1MultifactorResponseDetails multifactor) {
        if (multifactor == null) {
            return null;
        }

        return MultifactorInfoDTO.builder()
                                 .email(multifactor.getEmail())
                                 .method(multifactor.getMethod())
                                 .methods(multifactor.getMethods())
                                 .build();
    }

    private static HCaptchaDTO hCaptchaFromInternal(RsoAuthenticatorV1HCaptcha captcha) {
        if (captcha == null) {
            return null;
        }

        return HCaptchaDTO.builder()
                          .key(captcha.getKey())
                          .data(captcha.getData())
                          .build();
    }

    private static LoginStatusDTO loginStatusFromInternal(RsoAuthenticatorV1ResponseType type) {
        if (type == null) {
            return LoginStatusDTO.UNKNOWN;
        }

        switch (type) {
            case AUTH -> {
                return LoginStatusDTO.LOGGED_OUT;
            }
            case ERROR -> {
                return LoginStatusDTO.ERROR;
            }
            case MULTIFACTOR -> {
                return LoginStatusDTO.MULTIFACTOR_REQUIRED;
            }
            case SUCCESS -> {
                return LoginStatusDTO.LOGGED_IN;
            }
            default -> {
                return LoginStatusDTO.UNKNOWN;
            }
        }
    }

    @Override
    protected CompletableFuture<RsoAuthenticatorV1AuthenticationResponse> doFetchInitialData() {
        Optional<CoreSdkApi> optionalCoreSdkApi = riotClientService.getApi(CoreSdkApi.class);

        if (optionalCoreSdkApi.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to build CoreSdkApi client for RsoAuthenticationManager")
            );
        }

        CoreSdkApi coreSdkApi = optionalCoreSdkApi.get();

        final RsoAuthenticatorV1AuthenticationResponse resp;
        try {
            resp = coreSdkApi.rsoAuthenticatorV1AuthenticationGet();
        } catch (Exception e) {
            log.error(
                    "Failed to fetch initial data for RsoAuthenticationManager",
                    e
            );
            return CompletableFuture.failedFuture(e);
        }

        if (resp == null) {
            return CompletableFuture.failedFuture(
                    new Exception("Failed to fetch initial data for RsoAuthenticationManager")
            );
        }

        return CompletableFuture.completedFuture(resp);
    }

    @Override
    protected Matcher getUriMatcher(String uri) {
        return RSO_AUTHENTICATOR_V1_AUTHENTICATION_PATTERN.matcher(uri);
    }

    @Override
    protected void handleUpdate(
            RCUWebsocketMessage.MessageType type,
            JsonNode data,
            Matcher uriMatcher
    ) {

        switch (type) {
            case DELETE -> resetInternalState();
            case CREATE, UPDATE -> {
                Optional<RsoAuthenticatorV1AuthenticationResponse> updatedData = parseJson(
                        data,
                        RsoAuthenticatorV1AuthenticationResponse.class
                );
                if (updatedData.isEmpty()) {
                    log.error(
                            "Successfully matched pattern, but unable to parse JSON: {}",
                            data
                    );
                    return;
                }
                RsoAuthenticatorV1AuthenticationResponse authenticationResponse = updatedData.get();
                log.info(
                        "New Auth state {}",
                        updatedData.get()
                                   .getType()
                );
                setState(authenticationResponse);
            }
        }
    }
}
