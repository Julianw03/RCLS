package com.julianw03.rcls.service.rest.launch;

import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.generated.api.PluginProductLauncherApi;
import com.julianw03.rcls.generated.api.PluginRiotClientLifecycleApi;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1AuthenticationResponse;
import com.julianw03.rcls.generated.model.RsoAuthenticatorV1ResponseType;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.base.cacheService.rcu.RCUStateService;
import com.julianw03.rcls.service.base.cacheService.rcu.impl.RsoAuthenticationManager;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LaunchV1RestService implements LaunchV1Service {

    private final RiotClientService riotClientService;
    private final ProcessService    processService;
    private final RCUStateService   cacheService;

    @Autowired
    public LaunchV1RestService(
            RiotClientService riotClientService,
            ProcessService processService,
            RCUStateService cacheService
    ) {
        this.riotClientService = riotClientService;
        this.processService = processService;
        this.cacheService = cacheService;
    }

    public void launchRiotClientUx() throws ExecutionException {
        InternalApiResponse response = riotClientService.request(
                HttpMethod.POST,
                "/riot-client-lifecycle/v1/show",
                null
        );

        switch (response) {
            case InternalApiResponse.ApiError error -> {
                throw new ExecutionException(error.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new ExecutionException(exception.getException());
            }
            default -> {
                return;
            }
        }
    }

    public void hideRiotClientUx() throws ExecutionException {
        PluginRiotClientLifecycleApi riotClientLifecycleApi = riotClientService.getApi(PluginRiotClientLifecycleApi.class).orElseThrow(
                () -> new APIException("Failed to get riotClientLifecycleApi client")
        );

        try {
            riotClientLifecycleApi.riotClientLifecycleV1HidePost();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    public List<String> getOperatingSystemSupportedGames(SupportedGame.ResolveStrategy resolveStrategy) {
        SupportedGame.ResolveStrategy resolveStrategyEnum = Optional.ofNullable(resolveStrategy).orElseGet(SupportedGame.ResolveStrategy::getDefault);
        return processService.getSupportedGames().stream()
                .map(resolveStrategyEnum::getId)
                .toList();
    }

    public List<String> getAllSupportedGames(SupportedGame.ResolveStrategy resolveStrategy) {
        SupportedGame.ResolveStrategy resolveStrategyEnum = Optional.ofNullable(resolveStrategy).orElseGet(SupportedGame.ResolveStrategy::getDefault);

        return Arrays.stream(SupportedGame.values())
                .map(resolveStrategyEnum::getId)
                .toList();
    }

    public void launchGameWithPatchline(String gameId, String patchlineId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException {
        RsoAuthenticatorV1AuthenticationResponse rsoAuthState = cacheService.getObjectDataManger(RsoAuthenticationManager.class).getState();
        ServletUtils.assertEqual("VerifyLoggedIn", RsoAuthenticatorV1ResponseType.SUCCESS, rsoAuthState.getType());

        SupportedGame.ResolveStrategy resolveStrategy = Optional.ofNullable(lookupStrategy).orElseGet(SupportedGame.ResolveStrategy::getDefault);
        SupportedGame game = resolveStrategy.resolve(gameId).orElseThrow(() -> new IllegalArgumentException("Game with ID " + gameId + " not found"));

        if (patchlineId == null || patchlineId.isEmpty()) {
            throw new IllegalArgumentException("Patchline ID cannot be null or empty");
        }

        PluginProductLauncherApi productLauncherApi = riotClientService.getApi(PluginProductLauncherApi.class).orElseThrow(
                () -> new APIException("Failed to get CoreSdkApi client")
        );


        try {
            productLauncherApi.productLauncherV1ProductsProductIdPatchlinesPatchlineIdPost(
                    game.getRiotInternalName(),
                    patchlineId
            );
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

        hideRiotClientUx();
    }

    public void killGame(String gameId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException {
        SupportedGame.ResolveStrategy resolveStrategy = Optional.ofNullable(lookupStrategy).orElseGet(SupportedGame.ResolveStrategy::getDefault);
        SupportedGame game = resolveStrategy.resolve(gameId).orElseThrow(() -> new IllegalArgumentException("Game with ID " + gameId + " not found"));

        try {
            processService.killGameProcess(game).orTimeout(5, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }
}
