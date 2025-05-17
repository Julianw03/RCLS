package com.julianw03.rcls.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.api.*;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginV1RestService {
    private final RiotClientService riotClientService;
    private final CacheService      cacheService;
    private final ObjectMapper      mapper = new ObjectMapper();
    private final ObjectNode        RSO_AUTH_V2_GRANT_OBJECT;
    private final ObjectNode        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT;

    public static sealed class InternalRsoLoginResponse permits InternalRsoLoginResponse.Multifactor, InternalRsoLoginResponse.Success {
        @Getter
        public static final class Multifactor extends InternalRsoLoginResponse {
            private final RsoAuthenticatorV1AuthenticationResponse.MultifactorDetails multifactorDetails;

            public Multifactor(RsoAuthenticatorV1AuthenticationResponse.MultifactorDetails multifactorDetails) {
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
        this.RSO_AUTH_V2_GRANT_OBJECT = getRSO_AUTH_V2_GRANT_OBJECT(mapper);
        this.RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT = getRSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT(mapper);
    }

    private ObjectNode getRSO_AUTH_V2_GRANT_OBJECT(ObjectMapper mapper) {
        final ObjectNode node = mapper.createObjectNode();
        node.put("clientId", "riot-client");
        final ArrayNode trustLevels = mapper.createArrayNode();
        trustLevels.add("always_trusted");
        node.set("trustLevels", trustLevels);
        return node;
    }

    private ObjectNode getRSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT(ObjectMapper mapper) {
        final ObjectNode node = mapper.createObjectNode();
        node.put("language", "en_US");
        node.put("product-id", "riot-client");
        node.put("state", "auth");
        return node;
    }

    private ObjectNode createSessionLoginObject(String loginToken) {
        ObjectNode node = mapper.createObjectNode();
        node.put("authentication_type", "RiotAuth");
        node.put("login_token", loginToken);
        node.put("persist_login", false);
        return node;
    }

    public void reset() {
        InternalApiResponse grantsResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT
        );

        final RsoAuthAuthorizationResponse authAuthorizationResponse = ServletUtils.expectInternalApiSuccess(grantsResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthAuthorizationResponse> optionalResponse = success.map(mapper, RsoAuthAuthorizationResponse.class);
            if (optionalResponse.isEmpty()) {
                throw new APIException("Failed to parse RSO Auth Authorization response");
            }
            return optionalResponse.get();
        });

        ServletUtils.assertEqual(
                "RSO Authorizations",
                RsoAuthAuthorizationResponse.Type.NEEDS_AUTHENTICATION,
                authAuthorizationResponse.getType()
        );

        InternalApiResponse deleteAuthenticationResponse = riotClientService.request(
                HttpMethod.DELETE,
                "/rso-authenticator/v1/authentication",
                null
        );

        switch (deleteAuthenticationResponse) {
            case InternalApiResponse.ApiError apiError -> {
                throw new APIException(apiError.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new APIException(exception.getException());
            }
            default -> {
            }
        }
    }

    public RsoAuthenticatorV1AuthenticationResponse.Captcha.HCaptcha getCaptcha() {

        RsoAuthenticatorV1AuthenticationResponse internalAuthState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();

        ServletUtils.assertEqual("Assert Auth Status", "auth", internalAuthState.getType());

        InternalApiResponse authenticationResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-authenticator/v1/authentication/riot-identity/start",
                RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT
        );

        return ServletUtils.expectInternalApiSuccess(authenticationResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthenticatorV1AuthenticationResponse.Captcha.HCaptcha> optionalHCaptcha = success.map(mapper, RsoAuthenticatorV1AuthenticationResponse.class)
                    .map(RsoAuthenticatorV1AuthenticationResponse::getCaptcha)
                    .map(RsoAuthenticatorV1AuthenticationResponse.Captcha::getHcaptcha);

            return optionalHCaptcha.orElseThrow(() -> new APIException("Captcha not found"));
        });
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

        ServletUtils.assertEqual("ExpectAuthState", "auth", currentState.getType());

        InternalApiResponse authenticationResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-authenticator/v1/authentication/riot-identity/complete",
                input
        );

        return ServletUtils.expectInternalApiSuccess(authenticationResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthenticatorV1AuthenticationResponse> optionalAuthResponse = success.map(mapper, RsoAuthenticatorV1AuthenticationResponse.class);
            RsoAuthenticatorV1AuthenticationResponse authResponse = optionalAuthResponse.orElseThrow();
            final String error = authResponse.getError();
            if (error != null && !error.isBlank()) {
                throw new APIException("Request returned an non empty error " + error, HttpStatus.BAD_GATEWAY);
            }

            final RsoAuthenticatorV1AuthenticationResponse.Success successResponse = authResponse.getSuccess();

            if (successResponse == null || successResponse.getLogin_token() == null || successResponse.getLogin_token().isBlank()) {
                final RsoAuthenticatorV1AuthenticationResponse.MultifactorDetails multifactorDetails = authResponse.getMultifactor();
                if (multifactorDetails == null || multifactorDetails.getAuth_method() == null) {
                    throw new APIException("Invalid Multifactor response", HttpStatus.BAD_GATEWAY);
                }
                return new InternalRsoLoginResponse.Multifactor(multifactorDetails);
            }

            return resumeNormalAuthflow(authResponse);
        });
    }

    private InternalRsoLoginResponse.Success resumeNormalAuthflow(RsoAuthenticatorV1AuthenticationResponse authResponse) {
        String loginToken = authResponse.getSuccess().getLogin_token();

        InternalApiResponse rsoAuthSessionResponse = riotClientService.request(
                HttpMethod.PUT,
                "/rso-auth/v1/session/login-token",
                createSessionLoginObject(loginToken)
        );

        final RsoAuthSessionResponse authSessionResponse = ServletUtils.expectInternalApiSuccess(rsoAuthSessionResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthSessionResponse> optionalResponse = success.map(mapper, RsoAuthSessionResponse.class);
            if (optionalResponse.isEmpty()) {
                throw new APIException("Failed to parse RSO Auth Session response");
            }
            return optionalResponse.get();
        });

        ServletUtils.assertEqual(
                "RSO Login Token",
                RsoAuthSessionResponse.Type.AUTHENTICATED,
                authSessionResponse.getType()
        );

        InternalApiResponse rsoAuthorizationsResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT
        );

        final RsoAuthAuthorizationResponse authAuthorizationResponse = ServletUtils.expectInternalApiSuccess(rsoAuthorizationsResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthAuthorizationResponse> optionalResponse = success.map(mapper, RsoAuthAuthorizationResponse.class);
            if (optionalResponse.isEmpty()) {
                throw new APIException("Failed to parse RSO Auth Authorization response");
            }
            return optionalResponse.get();
        });

        ServletUtils.assertEqual(
                "RSO Authorizations",
                RsoAuthAuthorizationResponse.Type.AUTHORIZED,
                authAuthorizationResponse.getType()
        );

        return new InternalRsoLoginResponse.Success();
    }

    public InternalRsoLoginResponse.Success resolveMultifactor(RsoAuthenticatorV1MultifactorInput.Multifactor multifactorInput) {
        if (multifactorInput == null || multifactorInput.getRememberDevice() == null || multifactorInput.getOtp() == null) {
            throw new APIException("Invalid Input", HttpStatus.BAD_REQUEST, "Your object is null or missing required attributes");
        }

        final RsoAuthenticatorV1MultifactorInput riotClientRequestObject = new RsoAuthenticatorV1MultifactorInput();
        riotClientRequestObject.setMultifactor(multifactorInput);
        InternalApiResponse apiResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-authenticator/v1/authentication/multifactor",
                riotClientRequestObject
        );

        RsoAuthenticatorV1AuthenticationResponse response = ServletUtils.expectInternalApiSuccess(apiResponse, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthenticatorV1AuthenticationResponse> optionalResponse = success.map(mapper, RsoAuthenticatorV1AuthenticationResponse.class);
            if (optionalResponse.isEmpty()) {
                throw new APIException("Failed to parse authentication response");
            }
            return optionalResponse.get();
        });

        return resumeNormalAuthflow(response);
    }

    public void logout() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT
        );

        final RsoAuthAuthorizationResponse parsedResponse = ServletUtils.expectInternalApiSuccess(response, (InternalApiResponse.Success success) -> {
            Optional<RsoAuthAuthorizationResponse> optResponse = success.map(mapper, RsoAuthAuthorizationResponse.class);
            if (optResponse.isEmpty()) {
                throw new APIException("Failed to parse RSO Auth Authorization response");
            }
            return optResponse.get();
        });

        ServletUtils.assertEqual("VerifyLoggedIn", RsoAuthAuthorizationResponse.Type.AUTHORIZED, parsedResponse.getType());

        riotClientService.request(
                HttpMethod.DELETE,
                "/rso-auth/v1/session",
                null
        );

        if (!response.isSuccessful()) {
            throw new APIException("");
        }
    }
}
