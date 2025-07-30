package com.julianw03.rcls.service.rest.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.*;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.base.cacheService.ObjectDataManager;
import com.julianw03.rcls.service.base.cacheService.rcu.RCUStateService;
import com.julianw03.rcls.service.base.cacheService.rcu.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginV1ServiceImpl implements LoginV1Service {
    private final RiotClientService                            riotClientService;
    private final RCUStateService                              cacheService;
    private final ObjectMapper                                 mapper = new ObjectMapper();
    private final RsoAuthAuthorizationRequest                  AUTH_GRANT_REQUEST;
    private final RsoAuthenticatorV1RiotIdentityAuthStartInput IDENTITY_START_INPUT;

    public static sealed class InternalRsoLoginResponse permits InternalRsoLoginResponse.Multifactor, InternalRsoLoginResponse.Success {
        @Getter
        public static final class Multifactor extends InternalRsoLoginResponse {
            private final RsoAuthMultifactorDetails multifactorDetails;

            public Multifactor(RsoAuthMultifactorDetails multifactorDetails) {
                this.multifactorDetails = multifactorDetails;
            }
        }

        public static final class Success extends InternalRsoLoginResponse {
            public Success() {

            }
        }
    }

    @Autowired
    public LoginV1ServiceImpl(
            RiotClientService riotClientService,
            RCUStateService cacheService
    ) {
        this.riotClientService = riotClientService;
        this.cacheService = cacheService;
        this.AUTH_GRANT_REQUEST = new RsoAuthAuthorizationRequest();
        this.AUTH_GRANT_REQUEST.setClientId("riot-client");
        this.AUTH_GRANT_REQUEST.addClaimsItem("always_trusted");

        this.IDENTITY_START_INPUT = new RsoAuthenticatorV1RiotIdentityAuthStartInput();
        this.IDENTITY_START_INPUT.setLanguage("en_US");
        this.IDENTITY_START_INPUT.setProductId("riot-client");
        this.IDENTITY_START_INPUT.setState("auth");
    }

    private RsoAuthSessionLoginToken createSessionLoginObject(String loginToken) {
        RsoAuthSessionLoginToken rsoAuthSessionLoginToken = new RsoAuthSessionLoginToken();
        rsoAuthSessionLoginToken.setLoginToken(loginToken);
        rsoAuthSessionLoginToken.setPersistLogin(false);
        rsoAuthSessionLoginToken.setAuthenticationType(RsoAuthAuthenticationTypeEnum.RIOTAUTH);

        return rsoAuthSessionLoginToken;
    }

    public LoginStatusDTO getLoginStatus() throws ExecutionException {
        ObjectDataManager<RsoAuthenticatorV1AuthenticationResponse> dataManager = cacheService.getObjectDataManger(RsoAuthenticationManager.class);
        final RsoAuthenticatorV1AuthenticationResponse currentState = dataManager.getState();

        if (currentState != null) {
            return loginStatusFromInternal(currentState.getType());
        }

        CompletableFuture<Void> setupFuture = dataManager.setupInternalState().orTimeout(500, TimeUnit.MICROSECONDS);
        final RsoAuthenticatorV1AuthenticationResponse internalAuthState;
        try {
            setupFuture.join();
            internalAuthState = dataManager.getState();
            if (internalAuthState == null) {
                throw new IllegalStateException("Internal authentication state is null");
            }
        } catch (Exception e) {
            throw new ExecutionException("Failed to setup RSO Authentication Manager state", e);
        }
        return loginStatusFromInternal(internalAuthState.getType());
    }

    public void resetHCaptcha() throws ExecutionException, IllegalStateException {
        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthAuthorizationResponse grantsResponse;
        try {
            grantsResponse = coreSdkApiClient.rsoAuthV2AuthorizationsPost(
                    AUTH_GRANT_REQUEST
            );
        } catch (Exception e) {
            throw new APIException("Failed to get RSO Auth V2 Authorization response", e);
        }

        ServletUtils.assertEqual(
                "RSO Authorizations",
                RsoAuthAuthorizationResponseType.NEEDS_AUTHENTICATION,
                grantsResponse.getType()
        );


        try {
            coreSdkApiClient.rsoAuthenticatorV1AuthenticationDelete();
        } catch (Exception e) {
            throw new APIException("Failed to delete RSO Authenticator V1 Authentication", e);
        }
    }

    public HCaptchaDTO getHCaptcha() throws ExecutionException, IllegalStateException {
        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        RsoAuthenticatorV1AuthenticationResponse internalAuthState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();

        ServletUtils.assertEqual("Assert Auth Status", RsoAuthenticatorV1ResponseType.AUTH, internalAuthState.getType());

        final RsoAuthenticatorV1AuthenticationResponse authenticationResponse;

        try {
            authenticationResponse = coreSdkApiClient.rsoAuthenticatorV1AuthenticationRiotIdentityStartPost(
                    IDENTITY_START_INPUT
            );
        } catch (Exception e) {
            throw new APIException("Failed to get RSO Authenticator V1 Authentication response", e);
        }

        RsoAuthenticatorV1HCaptcha hcaptcha = Optional.ofNullable(authenticationResponse)
                .map(RsoAuthenticatorV1AuthenticationResponse::getCaptcha)
                .map(RsoAuthenticatorV1Captcha::getHcaptcha)
                .orElseThrow(() -> new ExecutionException(new IllegalStateException()));

        return HCaptchaDTO.builder()
                .data(hcaptcha.getData())
                .key(hcaptcha.getKey())
                .build();
    }

    public LoginStatusDTO login(LoginInputDTO input) throws ExecutionException, IllegalStateException, IllegalArgumentException, MultifactorRequiredException {
        if (input == null) {
            throw new IllegalArgumentException("Input is null");
        }

        ServletUtils.assertFieldsNotNull(
                input,
                LoginInputDTO::getUsername,
                LoginInputDTO::getPassword,
                LoginInputDTO::getCaptcha,
                LoginInputDTO::getRemember
        );

        RsoAuthenticatorV1AuthenticationResponse currentState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();

        ServletUtils.assertEqual("ExpectAuthState", RsoAuthenticatorV1ResponseType.AUTH, currentState.getType());

        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthenticatorV1AuthenticationResponse authenticationResponse;

        try {
            authenticationResponse = coreSdkApiClient.rsoAuthenticatorV1AuthenticationRiotIdentityCompletePost(
                    new RsoAuthenticatorV1RiotIdentityAuthCompleteInput()
                            .username(input.getUsername())
                            .password(input.getPassword())
                            .captcha(input.getCaptcha())
                            .remember(input.getRemember())
            );
        } catch (Exception e) {
            throw new ExecutionException("Failed to get RSO Authenticator V1 Authentication response", e);
        }

        final String error = authenticationResponse.getError();
        if (error != null && !error.isBlank()) {
            if ("auth_failure".equalsIgnoreCase(error)) {
                throw new ExecutionException(new IllegalArgumentException(error));
            }
            throw new ExecutionException("Request returned a non-empty error: " + error, new IllegalStateException());
        }

        final RsoAuthenticatorV1SuccessResponseDetails successResponse = authenticationResponse.getSuccess();

        if (successResponse == null || successResponse.getLoginToken() == null || successResponse.getLoginToken().isBlank()) {
            final RsoAuthenticatorV1MultifactorResponseDetails multifactorDetails = authenticationResponse.getMultifactor();
            if (multifactorDetails == null || multifactorDetails.getAuthMethod() == null) {
                throw new ExecutionException(new IllegalArgumentException("Auth method is null"));
            }
            throw new MultifactorRequiredException(
                    MultifactorInfoDTO.builder()
                            .email(multifactorDetails.getEmail())
                            .methods(multifactorDetails.getMethods())
                            .method(multifactorDetails.getMethod())
                            .build()
            );
        }

        return resumeNormalAuthflow(authenticationResponse);
    }

    private LoginStatusDTO resumeNormalAuthflow(RsoAuthenticatorV1AuthenticationResponse authResponse) throws ExecutionException {
        String loginToken = authResponse.getSuccess().getLoginToken();

        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthSessionResponse authSessionResponse;

        try {
            authSessionResponse = coreSdkApiClient.rsoAuthV1SessionLoginTokenPut(
                    createSessionLoginObject(loginToken)
            );
        } catch (Exception e) {
            throw new ExecutionException("Failed to get RSO Auth Session response", e);
        }


        ServletUtils.assertEqual(
                "RSO Auth Session",
                RsoAuthSessionResponseType.AUTHENTICATED,
                authSessionResponse.getType()
        );

        final RsoAuthAuthorizationResponse grantsResponse;
        try {
            grantsResponse = coreSdkApiClient.rsoAuthV2AuthorizationsPost(
                    AUTH_GRANT_REQUEST
            );
        } catch (Exception e) {
            throw new ExecutionException("Failed to get RSO Auth V2 Authorization response", e);
        }

        ServletUtils.assertEqual(
                "RSO Authorizations",
                RsoAuthAuthorizationResponseType.AUTHORIZED,
                grantsResponse.getType()
        );

        return LoginStatusDTO.LOGGED_IN;
    }

    public LoginStatusDTO loginWithMultifactor(MultifactorInputDTO multifactorInput) throws ExecutionException {
        ServletUtils.assertFieldsNotNull(
                multifactorInput,
                MultifactorInputDTO::getOtp,
                MultifactorInputDTO::getRememberDevice
        );

        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthenticatorV1AuthenticationResponse authenticationResponse;
        try {
            authenticationResponse = coreSdkApiClient.rsoAuthenticatorV1AuthenticationMultifactorPost(
                    new RsoAuthenticatorV1AuthenticateMultifactorInput()
                            .multifactor(
                                    new RsoAuthenticatorV1MultifactorInput()
                                            .otp(multifactorInput.getOtp())
                                            .rememberDevice(multifactorInput.getRememberDevice())
                            )
            );
        } catch (Exception e) {
            throw new ExecutionException("Failed to get RSO Authenticator V1 Authentication response", e);
        }

        return resumeNormalAuthflow(authenticationResponse);
    }

    public void logout() throws ExecutionException {
        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthAuthorizationResponse grantsResponse;
        try {
            grantsResponse = coreSdkApiClient.rsoAuthV2AuthorizationsPost(
                    AUTH_GRANT_REQUEST
            );
        } catch (Exception e) {
            throw new ExecutionException("Failed to get RSO Auth V2 Authorization response", e);
        }

        ServletUtils.assertEqual("VerifyLoggedIn", RsoAuthAuthorizationResponseType.AUTHORIZED, grantsResponse.getType());

        try {
            coreSdkApiClient.rsoAuthV1SessionDelete();
        } catch (Exception e) {
            throw new ExecutionException("Failed to delete RSO Auth V1 Session", e);
        }

        try {
            coreSdkApiClient.rsoAuthenticatorV1AuthenticationDelete();
        } catch (Exception e) {
            throw new ExecutionException("Failed to delete RSO Authenticator V1 Authentication", e);
        }
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
}
