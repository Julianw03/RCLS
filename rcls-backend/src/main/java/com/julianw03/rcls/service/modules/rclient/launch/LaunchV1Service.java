package com.julianw03.rcls.service.modules.rclient.launch;

import com.julianw03.rcls.model.FailFastException;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.process.NoSuchProcessException;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface LaunchV1Service {
    void launchRiotClientUx() throws ExecutionException;
    void hideRiotClientUx() throws ExecutionException;

    List<String> getOperatingSystemSupportedGames(SupportedGame.ResolveStrategy resolveStrategy);
    List<String> getAllSupportedGames(SupportedGame.ResolveStrategy resolveStrategy);

    void launchGameWithPatchline(String gameId, String patchlineId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException, IllegalArgumentException, FailFastException;
    void killGame(String gameId, SupportedGame.ResolveStrategy lookupStrategy) throws ExecutionException, IllegalArgumentException, NoSuchProcessException, FailFastException;
}
