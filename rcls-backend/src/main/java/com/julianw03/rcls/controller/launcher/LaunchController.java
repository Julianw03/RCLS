package com.julianw03.rcls.controller.launcher;

import com.julianw03.rcls.controller.FailFastException;
import com.julianw03.rcls.controller.errors.ApiProblem;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.modules.rclient.launch.LaunchV1Service;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/riotclient/launcher/v1")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "503",
                description = "Used when the user has not yet connected to the Riot Client",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiProblem.class)
                )
        )
})
public class LaunchController {

    private final LaunchV1Service launchV1ServiceImpl;

    @Autowired
    public LaunchController(
            LaunchV1Service launchV1ServiceImpl
    ) {
        this.launchV1ServiceImpl = launchV1ServiceImpl;
    }

    @PostMapping(value = "/client")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully launched the Riot Client UX"
            )
    })
    public ResponseEntity<Void> launchRiotClientUx() throws ExecutionException {
        launchV1ServiceImpl.launchRiotClientUx();

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping(value = "/games", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Returns a list of all supported games by the RCLS Application",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    public ResponseEntity<List<String>> getSupportedGames(@RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) {
        return ResponseEntity.ofNullable(launchV1ServiceImpl.getAllSupportedGames(lookupStrategy));
    }

    @GetMapping(value = "/games/supported", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Returns a list of games supported by the current operating system",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    public ResponseEntity<List<String>> getOsSupportedGames(@RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) {
        return ResponseEntity
                .ofNullable(launchV1ServiceImpl.getOperatingSystemSupportedGames(lookupStrategy));
    }

    @PostMapping(value = "/game/{gameId}")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully launched the game with the specified game ID"
            )
    })
    public ResponseEntity<Void> launchGame(@PathVariable String gameId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) throws FailFastException, ExecutionException {
        launchV1ServiceImpl.launchGameWithPatchline(gameId, "live", lookupStrategy);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping(value = "/game/{gameId}/{patchlineId}")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully launched the game with the specified game ID and patchline ID"
            )
    })
    public ResponseEntity<Void> launchGameWithPatchline(@PathVariable String gameId, @PathVariable String patchlineId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) throws FailFastException, ExecutionException {
        launchV1ServiceImpl.launchGameWithPatchline(gameId, patchlineId, lookupStrategy);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping(value = "/client")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully hidden the Riot Client UX"
            )
    })
    public ResponseEntity<Void> hideRiotClientUx() throws ExecutionException {
        launchV1ServiceImpl.hideRiotClientUx();
        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping(value = "/game/{gameId}")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully killed the game with the specified game ID"
            )
    })
    public ResponseEntity<Void> killGame(@PathVariable String gameId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) throws NoSuchProcessException, FailFastException, ExecutionException {
        launchV1ServiceImpl.killGame(gameId, lookupStrategy);

        return ResponseEntity
                .noContent()
                .build();
    }
}
