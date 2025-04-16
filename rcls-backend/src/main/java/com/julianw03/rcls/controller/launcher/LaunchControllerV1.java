package com.julianw03.rcls.controller.launcher;

import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.model.api.RsoAuthSessionResponse;
import com.julianw03.rcls.model.api.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.cacheService.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/riotclient/launcher/v1")
public class LaunchControllerV1 {

    private final RiotClientService riotClientService;
    private final ProcessService    processService;
    private final CacheService      cacheService;

    @Autowired
    public LaunchControllerV1(
            RiotClientService riotClientService,
            ProcessService processService,
            CacheService cacheService
    ) {
        this.riotClientService = riotClientService;
        this.processService = processService;
        this.cacheService = cacheService;
    }

    @PostMapping(value = "/client", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> launchRiotClientUx() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/riot-client-lifecycle/v1/show",
                null
        );

        switch (response) {
            case InternalApiResponse.ApiError error -> {
                throw new APIException(error.getError());
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

    @GetMapping(value = "/games")
    public ResponseEntity<List<SupportedGame>> getSupportedGames() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(List.of(SupportedGame.values()));
    }

    @PostMapping(value = "/game/{gameId}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> launchGame(@PathVariable String gameId) {
        return launchGameWithPatchline(gameId, "live");
    }

    @PostMapping(value = "/game/{gameId}/{patchlineId}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> launchGameWithPatchline(@PathVariable String gameId, @PathVariable String patchlineId) {
        RsoAuthenticatorV1AuthenticationResponse rsoAuthState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();;
        ServletUtils.assertEqual("VerifyLoggedIn","success", rsoAuthState.getType());

        Optional<SupportedGame> optSupportedGame = SupportedGame.fromString(gameId);
        if (optSupportedGame.isEmpty())
            throw new APIException("Invalid Request", "The provided gameId is not recognized", HttpStatus.BAD_REQUEST);

        //TODO: This is vulnerable to path injection, fix this
        if (patchlineId == null || patchlineId.isEmpty()) {
            throw new APIException("Invalid Request", "The provided patchlineId is invalid", HttpStatus.BAD_REQUEST);
        }

        SupportedGame game = optSupportedGame.get();

        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/product-launcher/v1/products/" + game.getRiotInternalName() + "/patchlines/" + patchlineId,
                null
        );

        ServletUtils.assertSuccessStatus("LaunchGame", response);

        hideRiotClientUx();

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping(value = "/client", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> hideRiotClientUx() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/riot-client-lifecycle/v1/hide",
                null
        );

        ServletUtils.assertSuccessStatus("Hide Riotclient UX", response);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping(value = "/game/{gameId}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> killGame(@PathVariable String gameId) {
        Optional<SupportedGame> optSupportedGame = SupportedGame.fromString(gameId);
        if (optSupportedGame.isEmpty())
            throw new APIException("Invalid Request", "The provided gameId is not recognized", HttpStatus.BAD_REQUEST);

        SupportedGame game = optSupportedGame.get();

        try {
            processService.killGameProcess(game).orTimeout(5, TimeUnit.SECONDS).join();
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        } catch (Exception e) {
            throw new APIException(e);
        }
    }
}
