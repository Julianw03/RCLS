package com.julianw03.rcls.controller.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.api.*;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.cacheService.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/riotclient/login/v1")
public class LoginControllerV1 {
    private final RiotClientService riotClientService;
    private final CacheService      cacheService;
    private final ObjectMapper      mapper = new ObjectMapper();
    private final ObjectNode        RSO_AUTH_V2_GRANT_OBJECT;
    private final ObjectNode        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT;

    @Autowired
    public LoginControllerV1(
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

    @PostMapping("/reset")
    public ResponseEntity<Void> resetLoginProcess() {
        //Authorizations
        final String rsoAuthEndpoint = "/rso-auth/v2/authorizations";

        Optional<RsoAuthAuthorizationResponse> optAuth = riotClientService.request(
                HttpMethod.POST,
                rsoAuthEndpoint,
                RSO_AUTH_V2_GRANT_OBJECT,
                RsoAuthAuthorizationResponse.class
        ).map(HttpResponse::body);

        if (optAuth.isEmpty()) {
            throw new APIException("Processing failed", "Failed to get data from " + rsoAuthEndpoint);
        }

        RsoAuthAuthorizationResponse rsoAuthAuthorizationResponse = optAuth.get();
        RsoAuthAuthorizationResponse.Type rsoAuthType = rsoAuthAuthorizationResponse.getType();
        RsoAuthAuthorizationResponse.Type expectedType = RsoAuthAuthorizationResponse.Type.NEEDS_AUTHENTICATION;
        ServletUtils.assertEqual("RSO Authorizations", expectedType, rsoAuthType);

        //Delete previous Authentication
        final String rsoAuthenticationEndpoint = "/rso-authenticator/v1/authentication";

        InternalApiResponse deleteAuthenticationResponse = riotClientService.request(
                HttpMethod.DELETE,
                rsoAuthenticationEndpoint,
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
                return ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .build();
            }
        }
    }

    @GetMapping("/captcha")
    public ResponseEntity<RsoAuthenticatorV1AuthenticationResponse.Captcha.HCaptcha> getCaptcha() {
        Optional<RsoAuthenticatorV1AuthenticationResponse.Captcha.HCaptcha> optHcaptcha = riotClientService.request(
                        HttpMethod.POST,
                        "/rso-authenticator/v1/authentication/riot-identity/start",
                        RSO_AUTHENTICATOR_RIOT_IDENTITY_START_OBJECT,
                        RsoAuthenticatorV1AuthenticationResponse.class
                ).map(HttpResponse::body)
                .map(RsoAuthenticatorV1AuthenticationResponse::getCaptcha)
                .map(RsoAuthenticatorV1AuthenticationResponse.Captcha::getHcaptcha);

        if (optHcaptcha.isEmpty()) {
            throw new APIException("Processing failed", "Failed to start Authentication process");
        }

        return ResponseEntity.ok(optHcaptcha.get());
    }

    @PostMapping("/login")
    public ResponseEntity<?> performLogin(@RequestBody RsoAuthenticatorV1RiotIdentityAuthCompleteInput body) {
        if (body == null)
            throw new APIException("Invalid Input", "Your object could not be mapped", HttpStatus.BAD_REQUEST);

        RsoAuthenticatorV1AuthenticationResponse currentState = cacheService.getObjectDataManger(RsoAuthenticationManager.class)
                .getState();
        ServletUtils.assertEqual("ExpectAuthState", "auth", currentState.getType());


        Optional<RsoAuthenticatorV1AuthenticationResponse> optResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-authenticator/v1/authentication/riot-identity/complete",
                body,
                RsoAuthenticatorV1AuthenticationResponse.class
        ).map(HttpResponse::body);

        if (optResponse.isEmpty()) {
            throw new APIException("Processing failed", "Failed to perform login Request");
        }

        RsoAuthenticatorV1AuthenticationResponse response = optResponse.get();

        if (response.getError() != null && !response.getError().isBlank()) {
            throw new APIException("Request returned an non empty error " + response.getError(), "Request returned an non empty error " + response.getError(), HttpStatus.BAD_GATEWAY);
        }

        if (response.getSuccess() == null || response.getSuccess().getLogin_token() == null || response.getSuccess().getLogin_token().isEmpty()) {
            if (response.getMultifactor() == null || response.getMultifactor().getAuth_method() == null) {
                throw new APIException("Processing Error", "");
            }
            return resumeMultifactorAuthflow(response);
        }

        return resumeNormalAuthflow(response);
    }

    private ResponseEntity<Void> resumeNormalAuthflow(RsoAuthenticatorV1AuthenticationResponse response) {
        String loginToken = response.getSuccess().getLogin_token();

        //Login Token
        InternalApiResponse rsoAuthSessionResponse = riotClientService.request(
                HttpMethod.PUT,
                "/rso-auth/v1/session/login-token",
                createSessionLoginObject(loginToken)
        );

        RsoAuthSessionResponse authSessionResponse;
        switch (rsoAuthSessionResponse) {
            case InternalApiResponse.Success success -> {
                Optional<RsoAuthSessionResponse> mapped = success.map(mapper, RsoAuthSessionResponse.class);
                if (mapped.isEmpty()) throw new APIException("Processing failed");
                authSessionResponse = mapped.get();
            }
            case InternalApiResponse.ApiError apiError -> {
                throw new APIException(apiError.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new APIException(exception.getException());
            }
            default -> throw new APIException("Unexpected Error Occurred");
        }


        ServletUtils.assertEqual(
                "RSO Login Token",
                RsoAuthSessionResponse.Type.AUTHENTICATED,
                authSessionResponse.getType()
        );

        //Authorizations
        final String rsoAuthEndpoint = "/rso-auth/v2/authorizations";
        Optional<RsoAuthAuthorizationResponse> optAuth = riotClientService.request(
                HttpMethod.POST,
                rsoAuthEndpoint,
                RSO_AUTH_V2_GRANT_OBJECT,
                RsoAuthAuthorizationResponse.class
        ).map(HttpResponse::body);

        if (optAuth.isEmpty()) {
            throw new APIException("Processing failed", "Failed to get data from " + rsoAuthEndpoint);
        }

        RsoAuthAuthorizationResponse rsoAuthAuthorizationResponse = optAuth.get();
        RsoAuthAuthorizationResponse.Type rsoAuthType = rsoAuthAuthorizationResponse.getType();

        ServletUtils.assertEqual(
                "RSO Authorizations",
                RsoAuthAuthorizationResponse.Type.AUTHORIZED,
                rsoAuthType
        );

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping(value = "/multifactor", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resolveMultifactor(@RequestBody RsoAuthenticatorV1MultifactorInput.Multifactor multifactorInput) {
        if (multifactorInput.getRememberDevice() == null || multifactorInput.getOtp() == null) {
            throw new APIException("Invalid Input", "Your object is missing required attributes", HttpStatus.BAD_REQUEST);
        }

        final RsoAuthenticatorV1MultifactorInput riotClientRequestObject = new RsoAuthenticatorV1MultifactorInput();
        riotClientRequestObject.setMultifactor(multifactorInput);

        InternalApiResponse apiResponse = riotClientService.request(
                HttpMethod.POST,
                "/rso-authenticator/v1/authentication/multifactor",
                riotClientRequestObject
        );

        RsoAuthenticatorV1AuthenticationResponse response;
        switch (apiResponse) {
            case InternalApiResponse.ApiError error -> {
                throw new APIException(error.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new APIException(exception.getException());
            }
            case InternalApiResponse.Success success -> {
                Optional<RsoAuthenticatorV1AuthenticationResponse> mappedResponse = success.map(mapper, RsoAuthenticatorV1AuthenticationResponse.class);
                if (mappedResponse.isEmpty()) throw new APIException("Processing failed");
                response = mappedResponse.get();
            }
            default -> {
                throw new APIException("Unexpected Error Occurred");
            }
        }

        return resumeNormalAuthflow(response);
    }

    @PostMapping("/logout")
    private ResponseEntity<Void> logout() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/rso-auth/v2/authorizations",
                RSO_AUTH_V2_GRANT_OBJECT
        );

        final RsoAuthAuthorizationResponse parsedResponse;
        if (Objects.requireNonNull(response) instanceof InternalApiResponse.Success success) {
            Optional<RsoAuthAuthorizationResponse> optResponse = success.map(mapper, RsoAuthAuthorizationResponse.class);
            parsedResponse = optResponse.orElseThrow();
        } else {
            throw new IllegalStateException("Unexpected value: " + response);
        }

        ServletUtils.assertEqual("VerifyLoggedIn", parsedResponse.getType(), RsoAuthAuthorizationResponse.Type.AUTHORIZED);

        riotClientService.request(
                HttpMethod.DELETE,
                "/rso-auth/v1/session",
                null
        );

        if (!response.isSuccessful()) {
            throw new APIException("");
        }

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    private ObjectNode createSessionLoginObject(String loginToken) {
        ObjectNode node = mapper.createObjectNode();
        node.put("authentication_type", "RiotAuth");
        node.put("login_token", loginToken);
        node.put("persist_login", false);
        return node;
    }

    private ResponseEntity<RsoAuthenticatorV1AuthenticationResponse.MultifactorDetails> resumeMultifactorAuthflow(RsoAuthenticatorV1AuthenticationResponse response) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response.getMultifactor());
    }
}
