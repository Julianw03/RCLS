package com.julianw03.rcls.service.rest;

import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.model.api.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.service.base.cacheService.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LaunchV1RestService {

    private final RiotClientService riotClientService;
    private final ProcessService    processService;
    private final CacheService      cacheService;

    @Autowired
    public LaunchV1RestService(
            RiotClientService riotClientService,
            ProcessService processService,
            CacheService cacheService
    ) {
        this.riotClientService = riotClientService;
        this.processService = processService;
        this.cacheService = cacheService;
    }

    public void launchRiotClientUx() {
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
                return;
            }
        }
    }

    public void hideRiotClientUx() {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/riot-client-lifecycle/v1/hide",
                null
        );

        ServletUtils.assertSuccessStatus("Hide Riotclient UX", response);
    }

    public List<String> getOperatingSystemSupportedGames(String resolveStrategy) {
        Optional<SupportedGame.ResolveStrategy> optResolveStrategy = SupportedGame.ResolveStrategy.fromString(resolveStrategy);
        if (optResolveStrategy.isEmpty())
            throw new APIException("Invalid Request", HttpStatus.BAD_REQUEST, "The provided resolveStrategy is not recognized");

        SupportedGame.ResolveStrategy resolveStrategyEnum = optResolveStrategy.get();
        return processService.getSupportedGames().stream()
                .map(resolveStrategyEnum::getId)
                .toList();
    }

    public List<String> getAllSupportedGames(String resolveStrategy) {
        Optional<SupportedGame.ResolveStrategy> optResolveStrategy = SupportedGame.ResolveStrategy.fromString(resolveStrategy);
        if (optResolveStrategy.isEmpty())
            throw new APIException("Invalid Request", HttpStatus.BAD_REQUEST, "The provided resolveStrategy is not recognized");

        SupportedGame.ResolveStrategy resolveStrategyEnum = optResolveStrategy.get();

        return Arrays.stream(SupportedGame.values())
                .map(resolveStrategyEnum::getId)
                .toList();
    }

    public void launchGameWithPatchline(String gameId, String patchlineId, String lookupStrategy) {
        RsoAuthenticatorV1AuthenticationResponse rsoAuthState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();;
        ServletUtils.assertEqual("VerifyLoggedIn", "success", rsoAuthState.getType());

        Optional<SupportedGame> optSupportedGame = SupportedGame.ResolveStrategy.fromString(lookupStrategy)
                .flatMap(resolveStrategy -> resolveStrategy.resolve(gameId));

        if (optSupportedGame.isEmpty())
            throw new APIException("Invalid Request", HttpStatus.BAD_REQUEST, "The provided gameId is not recognized");

        //TODO: This is vulnerable to path injection, fix this
        if (patchlineId == null || patchlineId.isEmpty()) {
            throw new APIException("Invalid Request", HttpStatus.BAD_REQUEST, "The provided patchlineId is invalid");
        }

        SupportedGame game = optSupportedGame.get();

        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/product-launcher/v1/products/" + game.getRiotInternalName() + "/patchlines/" + patchlineId,
                null
        );

        ServletUtils.assertSuccessStatus("LaunchGame", response);

        hideRiotClientUx();

        return;
    }

    public void killGame(String gameId, String lookupStrategy) {
        Optional<SupportedGame> optSupportedGame = SupportedGame.ResolveStrategy.fromString(lookupStrategy)
                .flatMap(resolveStrategy -> resolveStrategy.resolve(gameId));
        if (optSupportedGame.isEmpty())
            throw new APIException("Invalid Request", HttpStatus.BAD_REQUEST, "The provided gameId is not recognized");

        SupportedGame game = optSupportedGame.get();

        try {
            processService.killGameProcess(game).orTimeout(5, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            throw new APIException(e);
        }
    }
}
