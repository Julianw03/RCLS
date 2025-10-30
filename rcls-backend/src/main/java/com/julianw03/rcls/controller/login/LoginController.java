package com.julianw03.rcls.controller.login;

import com.julianw03.rcls.controller.errors.ApiProblem;
import com.julianw03.rcls.controller.errors.MultifactorRequiredProblem;
import com.julianw03.rcls.service.modules.rclient.login.LoginV1Service;
import com.julianw03.rcls.service.modules.rclient.login.model.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
                        schema = @Schema(implementation = ApiProblem.class)
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Generic processing error",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiProblem.class)
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
    public ResponseEntity<LoginStatusDTO> getLoginStatus() throws ExecutionException {
        final LoginStatusDTO loginStatusDTO = loginV1Service.getLoginStatus();
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
                                            implementation = ApiProblem.class
                                    )
                            )
                    ),
            }
    )
    @PostMapping(
            value = "/reset"
    )
    public ResponseEntity<Void> resetLoginProcess() throws ExecutionException, IllegalStateException {
        loginV1Service.resetHCaptcha();

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
                                    schema = @Schema(implementation = ApiProblem.class)
                            )
                    )
            }
    )
    @GetMapping(
            value = "/captcha",
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HCaptchaDTO> getCaptcha() throws ExecutionException, IllegalStateException {
        final HCaptchaDTO hCaptchaDTO = loginV1Service.getHCaptcha();
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
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = ApiProblem.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Multifactor authentication required",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = MultifactorRequiredProblem.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized, invalid credentials provided",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = ApiProblem.class
                                    )
                            )
                    ),
            }
    )
    @PostMapping(
            value = "/login",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginStatusDTO> performLogin(@RequestBody LoginInputDTO body) throws ExecutionException, MultifactorRequiredException, IllegalStateException, IllegalArgumentException{
        final LoginStatusDTO loginStatusDTO = loginV1Service.login(body);

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
                            responseCode = "401",
                            description = "Bad request, invalid multifactor code provided",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = ApiProblem.class
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict, probably due to already being logged in or an ongoing login process",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation =  ApiProblem.class
                                    )
                            )
                    )
            }
    )
    @PostMapping(
            value = "/multifactor",
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginStatusDTO> resolveMultifactor(@RequestBody MultifactorInputDTO multifactorInput) throws ExecutionException, IllegalStateException, IllegalArgumentException {
        final LoginStatusDTO loginStatusDTO = loginV1Service.loginWithMultifactor(multifactorInput);
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
                                            implementation = ApiProblem.class
                                    )
                            )
                    )
            }
    )
    @PostMapping("/logout")
    private ResponseEntity<Void> logout() throws ExecutionException, IllegalStateException {
        loginV1Service.logout();

        return ResponseEntity
                .noContent()
                .build();
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ApiProblem> handleIllegalArgumentException(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiProblem.fromProblemDetail(pd));
    }

    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<ApiProblem> handleIllegalStateException(IllegalStateException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiProblem.fromProblemDetail(pd));
    }

    @ExceptionHandler({MultifactorRequiredException.class})
    public ResponseEntity<MultifactorRequiredProblem> handleMultifactorRequiredException(MultifactorRequiredException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(MultifactorRequiredProblem.create(pd, e));
    }


}
