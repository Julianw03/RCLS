package com.julianw03.rcls.controller.launcher;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.rest.launch.LaunchV1Service;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
                        schema = @Schema(implementation = APIException.class)
                )
        )
})
public class LaunchControllerV1 {

    private final LaunchV1Service launchV1ServiceImpl;

    @Autowired
    public LaunchControllerV1(
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
    public ResponseEntity<Void> launchRiotClientUx() {
        try {
            launchV1ServiceImpl.launchRiotClientUx();
        } catch (ExecutionException e) {
            throw new APIException(e);
        }

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
    public ResponseEntity<Void> launchGame(@PathVariable String gameId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) {
        try {
            launchV1ServiceImpl.launchGameWithPatchline(gameId, "live", lookupStrategy);
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalArgumentException e) {
            throw APIException.builder(HttpStatus.BAD_REQUEST)
                    .details(e.getMessage())
                    .build();
        }
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
    public ResponseEntity<Void> launchGameWithPatchline(@PathVariable String gameId, @PathVariable String patchlineId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) {
        try {
            launchV1ServiceImpl.launchGameWithPatchline(gameId, patchlineId, lookupStrategy);
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalArgumentException e) {
            throw APIException.builder(HttpStatus.BAD_REQUEST)
                    .details(e.getMessage())
                    .build();
        }

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
    public ResponseEntity<Void> hideRiotClientUx() {
        try {
            launchV1ServiceImpl.hideRiotClientUx();
        } catch (ExecutionException e) {
            throw new APIException(e);
        }
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
    public ResponseEntity<Void> killGame(@PathVariable String gameId, @RequestParam(required = false) SupportedGame.ResolveStrategy lookupStrategy) {
        try {
            launchV1ServiceImpl.killGame(gameId, lookupStrategy);
        } catch (ExecutionException e) {
            throw new APIException(e);
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}
