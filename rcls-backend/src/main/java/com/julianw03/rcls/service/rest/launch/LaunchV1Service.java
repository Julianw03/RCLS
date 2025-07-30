package com.julianw03.rcls.service.rest.launch;

import com.julianw03.rcls.model.SupportedGame;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface LaunchV1Service {
    void launchRiotClientUx() throws ExecutionException;
    void hideRiotClientUx() throws ExecutionException;

    List<String> getOperatingSystemSupportedGames(SupportedGame.ResolveStrategy resolveStrategy);
    List<String> getAllSupportedGames(SupportedGame.ResolveStrategy resolveStrategy);

    void launchGameWithPatchline(String gameId, String patchlineId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException, IllegalArgumentException;
    void killGame(String gameId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException, IllegalArgumentException;
}
