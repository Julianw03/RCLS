package com.julianw03.rcls.service.process;

import com.julianw03.rcls.config.ServiceConfig;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.BaseService;

import java.util.concurrent.CompletableFuture;

public abstract class ProcessService extends BaseService {

    protected final ServiceConfig.ProcessServiceConfig config;
    protected final SupportedOperatingSystem           operatingSystem;

    protected ProcessService(
            SupportedOperatingSystem operatingSystem,
            ServiceConfig.ProcessServiceConfig config
    ) {
        this.operatingSystem = operatingSystem;
        this.config = config;
    }

    protected ServiceConfig.ProcessServiceConfig.OSExecutableMappings getOsExecutableNames() {
        return config.getExecutables().get(operatingSystem);
    }

    public abstract CompletableFuture<Void> startRiotClientServices(RiotClientConnectionParameters parameters);

    public abstract CompletableFuture<Void> killRiotClientServices();

    public abstract CompletableFuture<Void> killGameProcess(SupportedGame game);

    public abstract CompletableFuture<Void> killRiotClientProcess();
}
