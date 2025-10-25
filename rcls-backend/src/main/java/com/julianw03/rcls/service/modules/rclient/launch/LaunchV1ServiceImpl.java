package com.julianw03.rcls.service.modules.rclient.launch;

import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.generated.api.PluginProductLauncherApi;
import com.julianw03.rcls.generated.api.PluginRiotClientLifecycleApi;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.FailFastException;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.modules.rclient.login.RsoAuthenticationManager;
import com.julianw03.rcls.service.modules.rclient.login.model.AuthenticationStateDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class LaunchV1ServiceImpl implements LaunchV1Service {

    private final RiotClientService        riotClientService;
    private final ProcessService           processService;
    private final RsoAuthenticationManager authenticationManager;

    @Autowired
    public LaunchV1ServiceImpl(
            RiotClientService riotClientService,
            ProcessService processService,
            RsoAuthenticationManager rsoAuthenticationManager
    ) {
        this.riotClientService = riotClientService;
        this.processService = processService;
        this.authenticationManager = rsoAuthenticationManager;
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
        PluginRiotClientLifecycleApi riotClientLifecycleApi = riotClientService.getApi(PluginRiotClientLifecycleApi.class)
                                                                               .orElseThrow(
                                                                                       () -> new APIException("Failed to get riotClientLifecycleApi client")
                                                                               );

        try {
            riotClientLifecycleApi.riotClientLifecycleV1HidePost();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    public List<String> getOperatingSystemSupportedGames(SupportedGame.ResolveStrategy resolveStrategy) {
        SupportedGame.ResolveStrategy resolveStrategyEnum = Optional.ofNullable(resolveStrategy)
                                                                    .orElseGet(SupportedGame.ResolveStrategy::getDefault);
        return processService.getSupportedGames()
                             .stream()
                             .map(resolveStrategyEnum::getId)
                             .toList();
    }

    public List<String> getAllSupportedGames(SupportedGame.ResolveStrategy resolveStrategy) {
        SupportedGame.ResolveStrategy resolveStrategyEnum = Optional.ofNullable(resolveStrategy)
                                                                    .orElseGet(SupportedGame.ResolveStrategy::getDefault);

        return Arrays.stream(SupportedGame.values())
                     .map(resolveStrategyEnum::getId)
                     .toList();
    }

    public void launchGameWithPatchline(
            String gameId,
            String patchlineId,
            SupportedGame.ResolveStrategy lookupStrategy
    ) throws ExecutionException {
        AuthenticationStateDTO rsoAuthState = authenticationManager.getView();
        ServletUtils.assertEqual(
                "VerifyLoggedIn",
                LoginStatusDTO.LOGGED_IN,
                rsoAuthState.getLoginStatus()
        );

        SupportedGame.ResolveStrategy resolveStrategy = Optional.ofNullable(lookupStrategy)
                                                                .orElseGet(SupportedGame.ResolveStrategy::getDefault);
        SupportedGame game = resolveStrategy.resolve(gameId)
                                            .orElseThrow(() -> new IllegalArgumentException("Game with ID " +
                                                                                            gameId +
                                                                                            " not found"));

        if (patchlineId == null || patchlineId.isEmpty()) {
            throw new IllegalArgumentException("Patchline ID cannot be null or empty");
        }

        PluginProductLauncherApi productLauncherApi = riotClientService.getApi(PluginProductLauncherApi.class)
                                                                       .orElseThrow(
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

    public void killGame(
            String gameId,
            SupportedGame.ResolveStrategy lookupStrategy
    ) throws FailFastException, IllegalArgumentException, NoSuchProcessException {
        SupportedGame.ResolveStrategy resolveStrategy = Optional.ofNullable(lookupStrategy)
                                                                .orElseGet(SupportedGame.ResolveStrategy::getDefault);
        SupportedGame game = resolveStrategy.resolve(gameId)
                                            .orElseThrow(() -> new IllegalArgumentException("Game with ID " +
                                                                                            gameId +
                                                                                            " does not exist or is currently not supported by this Application"));

        processService.killGameProcess(game);
    }
}
