package com.julianw03.rcls.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.model.*;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginV1RestService {
    private final RiotClientService                            riotClientService;
    private final CacheService                                 cacheService;
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
    public LoginV1RestService(
            RiotClientService riotClientService,
            CacheService cacheService
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

    public void reset() {
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

    public RsoAuthenticatorV1HCaptcha getCaptcha() {
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

        return authenticationResponse.getCaptcha().getHcaptcha();
    }

    public InternalRsoLoginResponse performLogin(RsoAuthenticatorV1RiotIdentityAuthCompleteInput input) {
        if (input == null) {
            throw new APIException("Input cannot be null", HttpStatus.BAD_REQUEST);
        }

        ServletUtils.assertFieldsNotNull(
                input,
                RsoAuthenticatorV1RiotIdentityAuthCompleteInput::getUsername,
                RsoAuthenticatorV1RiotIdentityAuthCompleteInput::getPassword,
                RsoAuthenticatorV1RiotIdentityAuthCompleteInput::getCaptcha
        );

        RsoAuthenticatorV1AuthenticationResponse currentState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();

        ServletUtils.assertEqual("ExpectAuthState", RsoAuthenticatorV1ResponseType.AUTH, currentState.getType());

        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthenticatorV1AuthenticationResponse authenticationResponse;

        try {
            authenticationResponse = coreSdkApiClient.rsoAuthenticatorV1AuthenticationRiotIdentityCompletePost(
                    input
            );
        } catch (Exception e) {
            throw new APIException("Failed to get RSO Authenticator V1 Authentication response", e);
        }

        final String error = authenticationResponse.getError();
        if (error != null && !error.isBlank()) {
            throw new APIException("Request returned an non empty error " + error, HttpStatus.BAD_GATEWAY);
        }

        final RsoAuthenticatorV1SuccessResponseDetails successResponse = authenticationResponse.getSuccess();

        if (successResponse == null || successResponse.getLoginToken() == null || successResponse.getLoginToken().isBlank()) {
            final RsoAuthenticatorV1MultifactorResponseDetails multifactorDetails = authenticationResponse.getMultifactor();
            if (multifactorDetails == null || multifactorDetails.getAuthMethod() == null) {
                throw new APIException("Invalid Multifactor response", HttpStatus.BAD_GATEWAY);
            }
            return new InternalRsoLoginResponse.Multifactor(
                    new RsoAuthMultifactorDetails()
                            .email(multifactorDetails.getEmail())
                            .methods(multifactorDetails.getMethods())
            );
        }

        return resumeNormalAuthflow(authenticationResponse);

    }

    private InternalRsoLoginResponse.Success resumeNormalAuthflow(RsoAuthenticatorV1AuthenticationResponse authResponse) {
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
            throw new APIException("Failed to get RSO Auth Session response", e);
        }


        ServletUtils.assertEqual(
                "RSO Login Token",
                RsoAuthSessionResponseType.AUTHENTICATED,
                authSessionResponse.getType()
        );

        try {

        } catch (Exception e) {
            throw new APIException("Failed to get RSO Auth Session response", e);
        }

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
                RsoAuthAuthorizationResponseType.AUTHORIZED,
                grantsResponse.getType()
        );

        return new InternalRsoLoginResponse.Success();
    }

    public InternalRsoLoginResponse.Success resolveMultifactor(RsoAuthenticatorV1MultifactorInput multifactorInput) {
        if (multifactorInput == null || multifactorInput.getOtp() == null) {
            throw new APIException("Invalid Input", HttpStatus.BAD_REQUEST, "Your object is null or missing required attributes");
        }

        CoreSdkApi coreSdkApiClient = riotClientService.getApi(CoreSdkApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );

        final RsoAuthenticatorV1AuthenticationResponse authenticationResponse;
        try {
            authenticationResponse = coreSdkApiClient.rsoAuthenticatorV1AuthenticationMultifactorPost(
                    new RsoAuthenticatorV1AuthenticateMultifactorInput().multifactor(multifactorInput)
            );
        } catch (Exception e) {
            throw new APIException("Failed to get RSO Authenticator V1 Authentication response", e);
        }

        return resumeNormalAuthflow(authenticationResponse);
    }

    public void logout() {
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

        ServletUtils.assertEqual("VerifyLoggedIn", RsoAuthAuthorizationResponseType.AUTHORIZED, grantsResponse.getType());

        try {
            coreSdkApiClient.rsoAuthV1SessionDelete();
        } catch (Exception e) {
            throw new APIException("Failed to delete RSO Auth V1 Session", e);
        }
    }
}
