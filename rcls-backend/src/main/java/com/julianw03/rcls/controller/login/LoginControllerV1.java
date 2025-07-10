package com.julianw03.rcls.controller.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1HCaptcha;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1MultifactorInput;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1ResponseType;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1RiotIdentityAuthCompleteInput;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.rest.LoginV1RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/riotclient/login/v1")
public class LoginControllerV1 {
    private final LoginV1RestService loginV1RestService;
    private final ObjectMapper       mapper = new ObjectMapper();

    @Autowired
    public LoginControllerV1(
            LoginV1RestService loginV1RestService
    ) {
        this.loginV1RestService = loginV1RestService;
    }

    @GetMapping(
            value = "/status",
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RsoAuthenticatorV1ResponseType> getLoginStatus() {
        return ResponseEntity
                .ok()
                .body(loginV1RestService.getLoginStatus());
    }

    @PostMapping(
            value = "/reset"
    )
    public ResponseEntity<Void> resetLoginProcess() {
        loginV1RestService.reset();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping(
            value = "/captcha",
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RsoAuthenticatorV1HCaptcha> getCaptcha() {
        return ResponseEntity
                .ofNullable(loginV1RestService.getCaptcha());
    }

    @PostMapping(
            value = "/login",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> performLogin(@RequestBody RsoAuthenticatorV1RiotIdentityAuthCompleteInput body) {
        LoginV1RestService.InternalRsoLoginResponse internalRsoLoginResponse = loginV1RestService.performLogin(body);
        switch (internalRsoLoginResponse) {
            case LoginV1RestService.InternalRsoLoginResponse.Success success -> {
                return ResponseEntity
                        .ok()
                        .build();
            }
            case LoginV1RestService.InternalRsoLoginResponse.Multifactor multifactor -> {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(multifactor.getMultifactorDetails());
            }
            default -> {
                throw new APIException("Unexpected Error Occurred");
            }
        }
    }

    @PostMapping(
            value = "/multifactor",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> resolveMultifactor(@RequestBody RsoAuthenticatorV1MultifactorInput multifactorInput) {
        loginV1RestService.resolveMultifactor(multifactorInput);
        return ResponseEntity
                .ok()
                .build();
    }

    @PostMapping("/logout")
    private ResponseEntity<Void> logout() {
        loginV1RestService.logout();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
