package com.julianw03.rcls.service.riotclient.connection;

import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.controller.FailFastException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import com.julianw03.rcls.service.process.ProcessService;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * This class will try to kill all Processes that are known to RCLS and are dependent on
 * the Riot Client Services.
 * After killing all Processes it will start an own Riot Client Service instance with randomly
 * generated {@link RiotClientConnectionParameters}.
 * This allows for a "headless" connection with the Riot Client Services (no UX-Process required)
 *
 */
@Slf4j
public class ProcessTakeoverConnectionStrategy implements RiotClientConnectionStrategy {

    private final ProcessService processService;

    public ProcessTakeoverConnectionStrategy(
            ProcessService processService
    ) {
        this.processService = processService;
    }

    @Override
    public RiotClientConnectionParameters connect() throws Exception {
        RiotClientConnectionParameters parameters = generateParameters();
        log.debug(
                "Generated parameters: {}",
                parameters
        );
        CompletableFuture<?>[] killGameFutures = new CompletableFuture<?>[SupportedGame.values().length];

        int i = 0;
        for (SupportedGame game : SupportedGame.values()) {
            killGameFutures[i] = CompletableFuture.runAsync(() -> {
                                                      try {
                                                          processService.killGameProcess(game);
                                                      } catch (NoSuchProcessException | FailFastException e) {
                                                          throw new RuntimeException(e);
                                                      }
                                                  })
                                                  .exceptionally(ex -> {
                                                      if (ex instanceof CompletionException) {
                                                          if (ex.getCause() instanceof UnsupportedOperationException) {
                                                              log.info(
                                                                      "Game {} is not supported on this OS",
                                                                      game
                                                              );
                                                          }
                                                          return null;
                                                      }
                                                      throw new RuntimeException(
                                                              "Failed to kill game process",
                                                              ex
                                                      );
                                                  });
            i++;
        }

        try {
            CompletableFuture.allOf(killGameFutures)
                             .join();
        } catch (CancellationException | CompletionException e) {
            log.error(
                    "Failed to kill all running games",
                    e
            );
            throw new ExecutionException(
                    "Failed to kill all running games",
                    e
            );
        }
        log.debug("Successfully killed all games");

        try {
            processService.killRiotClientProcess();
        } catch (Exception e) {
            log.error(
                    "Failed to kill current Riot Client instance",
                    e
            );
            throw new ExecutionException(
                    "Failed to kill previous Riot Client Instance",
                    e
            );
        }
        log.debug("Successfully killed previous Riot Client instance");

        try {
            processService.killRiotClientServices();
        } catch (NoSuchProcessException | FailFastException e) {
            log.error(
                    "Failed to kill current Riot Client services instance",
                    e
            );
            throw new ExecutionException(
                    "Failed to kill previous RiotClientServices Instance",
                    e
            );
        }
        log.debug("Killed previous Riot Client Services instance");

        try {
            processService.startRiotClientServices(parameters);
        } catch (FailFastException e) {
            throw new ExecutionException(
                    "Failed to start Riot Client Services with own parameters",
                    e
            );
        }
        log.debug("Created new Riot Client Service Instance with current Application as Manager");
        return parameters;
    }

    @Override
    public void disconnect() throws Exception {

    }

    private RiotClientConnectionParameters generateParameters() {
        Integer port = Utils.getFreePort();
        SecureRandom random = new SecureRandom();
        byte[] randomToGenerate = new byte[18];
        random.nextBytes(randomToGenerate);
        return new RiotClientConnectionParameters(
                Base64.getEncoder()
                      .encodeToString(randomToGenerate),
                port
        );
    }
}
