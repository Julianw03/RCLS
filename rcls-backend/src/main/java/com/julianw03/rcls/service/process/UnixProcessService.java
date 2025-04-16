package com.julianw03.rcls.service.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.config.ServiceConfig;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//TODO: Implement LinuxProcessService
public class UnixProcessService extends ProcessService {
    private static final Logger log = LoggerFactory.getLogger(UnixProcessService.class);


    private final ScheduledExecutorService executorService;
    private final ObjectMapper             mapper;

    public UnixProcessService(SupportedOperatingSystem os, ServiceConfig.ProcessServiceConfig config) {
        super(os, config);
        mapper = new ObjectMapper();
        executorService = Executors.newScheduledThreadPool(10);
    }

    @Override
    public CompletableFuture<Void> startRiotClientServices(RiotClientConnectionParameters parameters) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> killRiotClientServices() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> killGameProcess(SupportedGame game) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> killRiotClientProcess() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void startup() {
        log.info("Startup called");
        log.info("{}", getOsExecutableNames());
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        log.info("Startup succeeded after {}ms", end - start);
    }

    @Override
    public void shutdown() {
        log.info("Shutdown called");
        long start = System.currentTimeMillis();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Graceful shutdown failed", e);
            executorService.shutdownNow();
        }
        long end = System.currentTimeMillis();
        log.info("Shutdown succeeded after {}ms", (end - start));
    }
}
