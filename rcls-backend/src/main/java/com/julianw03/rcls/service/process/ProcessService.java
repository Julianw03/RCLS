package com.julianw03.rcls.service.process;

import com.julianw03.rcls.controller.FailFastException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;

import java.util.List;

public interface ProcessService {
    void startRiotClientServices(RiotClientConnectionParameters parameters) throws FailFastException;
    void killRiotClientServices() throws NoSuchProcessException, FailFastException;
    void killRiotClientProcess() throws NoSuchProcessException, FailFastException;

    List<SupportedGame> getSupportedGames();

    void killGameProcess(SupportedGame supportedGame) throws NoSuchProcessException, FailFastException, UnsupportedOperationException;
}
