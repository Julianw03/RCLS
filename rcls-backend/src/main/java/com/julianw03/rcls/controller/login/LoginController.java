package com.julianw03.rcls.controller.login;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.rest.login.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/riotclient/login/v1")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "503",
                description = "Used when the user has not yet connected to the Riot Client",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = APIException.class)
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Generic processing error",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = APIException.class)
                )
        )
})
public class LoginController {
    private final LoginV1Service loginV1Service;

    @Autowired
    public LoginController(
            LoginV1Service loginV1Service
    ) {
        this.loginV1Service = loginV1Service;
    }

    @GetMapping(
            value = "/status",
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved login status",
                    content = @Content(
                            mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginStatusDTO.class)
                    )
            )
    })
    public ResponseEntity<LoginStatusDTO> getLoginStatus() {
        final LoginStatusDTO loginStatusDTO;
        try {
            loginStatusDTO = loginV1Service.getLoginStatus();
        } catch (ExecutionException e) {
            throw new APIException(e);
        }
        return ResponseEntity
                .ok()
                .body(loginStatusDTO);
    }

    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "204",
                            description = "Reset the login process"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    ),
            }
    )
    @PostMapping(
            value = "/reset"
    )
    public ResponseEntity<Void> resetLoginProcess() {
        try {
            loginV1Service.resetHCaptcha();
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalStateException e) {
            throw APIException.builder(HttpStatus.CONFLICT)
                    .details(e.getMessage())
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved HCaptcha data",
                            content = @Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = HCaptchaDTO.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = APIException.class)
                            )
                    )
            }
    )
    @GetMapping(
            value = "/captcha",
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HCaptchaDTO> getCaptcha() {
        final HCaptchaDTO hCaptchaDTO;
        try {
            hCaptchaDTO = loginV1Service.getHCaptcha();
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalStateException e) {
            throw APIException.builder(HttpStatus.CONFLICT)
                    .details(e.getMessage())
                    .build();
        }
        return ResponseEntity
                .ok()
                .body(hCaptchaDTO);
    }

    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully logged in"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad request, possibly due to invalid input",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Multifactor authentication required",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = MultifactorInfoDTO.class
                                    )
                            )
                    )
            }
    )
    @PostMapping(
            value = "/login",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> performLogin(@RequestBody LoginInputDTO body) {
        final LoginStatusDTO loginStatusDTO;
        try {
            loginStatusDTO = loginV1Service.login(body);
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (MultifactorRequiredException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMultifactorInfo());
        } catch (IllegalStateException e) {
            throw APIException.builder(HttpStatus.CONFLICT)
                    .details(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            throw APIException.builder(HttpStatus.BAD_REQUEST)
                    .details(e.getMessage())
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(loginStatusDTO);
    }

    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully resolved multifactor authentication"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad request, possibly due to invalid input",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    )
            }
    )
    @PostMapping(
            value = "/multifactor",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginStatusDTO> resolveMultifactor(@RequestBody MultifactorInputDTO multifactorInput) {
        final LoginStatusDTO loginStatusDTO;
        try {
            loginStatusDTO = loginV1Service.loginWithMultifactor(multifactorInput);
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalStateException e) {
            throw APIException.builder(HttpStatus.CONFLICT)
                    .details(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            throw APIException.builder(HttpStatus.BAD_REQUEST)
                    .details(e.getMessage())
                    .build();
        }

        return ResponseEntity
                .ok(loginStatusDTO);
    }

    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "204",
                            description = "Successfully logged out"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to not being logged in or an ongoing logout process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = APIException.class
                                    )
                            )
                    )
            }
    )
    @PostMapping("/logout")
    private ResponseEntity<Void> logout() {
        try {
            loginV1Service.logout();
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalStateException e) {
            throw APIException.builder(HttpStatus.CONFLICT)
                    .details(e.getMessage())
                    .build();
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}
