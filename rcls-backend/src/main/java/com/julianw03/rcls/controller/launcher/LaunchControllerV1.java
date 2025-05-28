package com.julianw03.rcls.controller.launcher;

import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.rest.LaunchV1RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/riotclient/launcher/v1")
public class LaunchControllerV1 {

    private final LaunchV1RestService launchV1RestService;
    private final StringHttpMessageConverter stringHttpMessageConverter;

    @Autowired
    public LaunchControllerV1(
            LaunchV1RestService launchV1RestService,
            StringHttpMessageConverter stringHttpMessageConverter) {
        this.launchV1RestService = launchV1RestService;
        this.stringHttpMessageConverter = stringHttpMessageConverter;
    }

    @PostMapping(value = "/client")
    public ResponseEntity<Void> launchRiotClientUx() {
        launchV1RestService.launchRiotClientUx();
        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping(value = "/games", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSupportedGames(@RequestParam(required = false) String lookupStrategy) {
        return ResponseEntity.ofNullable(launchV1RestService.getAllSupportedGames(lookupStrategy));
    }

    @GetMapping(value =  "/games/supported", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getOsSupportedGames(@RequestParam(required = false) String lookupStrategy) {
        return ResponseEntity
                .ofNullable(launchV1RestService.getOperatingSystemSupportedGames(lookupStrategy));
    }

    @PostMapping(value = "/game/{gameId}")
    public ResponseEntity<Void> launchGame(@PathVariable String gameId, @RequestParam(required = false) String lookupStrategy) {
        launchV1RestService.launchGameWithPatchline(gameId, "live", lookupStrategy);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping(value = "/game/{gameId}/{patchlineId}")
    public ResponseEntity<Void> launchGameWithPatchline(@PathVariable String gameId, @PathVariable String patchlineId, @RequestParam(required = false) String lookupStrategy) {
        launchV1RestService.launchGameWithPatchline(gameId, patchlineId, lookupStrategy);
        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping(value = "/client")
    public ResponseEntity<Void> hideRiotClientUx() {
        launchV1RestService.hideRiotClientUx();
        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping(value = "/game/{gameId}")
    public ResponseEntity<Void> killGame(@PathVariable String gameId, @RequestParam(required = false) String lookupStrategy) {
        launchV1RestService.killGame(gameId, lookupStrategy);
        return ResponseEntity
                .noContent()
                .build();
    }
}
