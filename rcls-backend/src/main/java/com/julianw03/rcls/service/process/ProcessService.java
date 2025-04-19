package com.julianw03.rcls.service.process;

import com.julianw03.rcls.config.ServiceConfig;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.BaseService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
public abstract class ProcessService extends BaseService {

    protected final ServiceConfig.ProcessServiceConfig config;
    protected final OperatingSystem                    operatingSystem;
    protected final ScheduledExecutorService           executorService;
    protected final AtomicReference<Process>           currentRCSProcess;
    protected       Path                               rcsPath;

    protected ProcessService(
            OperatingSystem operatingSystem,
            ServiceConfig.ProcessServiceConfig config
    ) {
        this.operatingSystem = operatingSystem;
        this.config = config;
        this.executorService = Executors.newScheduledThreadPool(10);
        this.currentRCSProcess = new AtomicReference<>();
    }

    protected ServiceConfig.ProcessServiceConfig.OSExecutableMappings getOsExecutableNames() {
        return config.getExecutables().get(operatingSystem);
    }

    @Override
    public final void startup() {
        log.info("Startup called");
        long start = System.currentTimeMillis();
        rcsPath = getRiotClientServicesExecutablePath().orElseThrow(() -> new IllegalStateException("Riot Client Services executable path could not be found"));
        log.info("Riot Client Services executable path: {}", rcsPath);
        doStartup();
        long end = System.currentTimeMillis();
        log.info("Startup succeeded after {}ms", end - start);
    }

    @Override
    public final void shutdown() {
        long start = System.currentTimeMillis();
        try {
            doShutdown();
        } catch (Exception e) {
            log.error("doShutdown threw Exception, will proceed with rest of shutdown flow", e);
        }
        Optional.ofNullable(currentRCSProcess.getAndSet(null)).ifPresent(Process::destroyForcibly);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Graceful shutdown failed", e);
            executorService.shutdownNow();
        }
        executorService.shutdown();
        long end = System.currentTimeMillis();
        log.info("Shutdown succeeded after {}ms", (end - start));
    }

    /**
     * May be used by subclasses to perform any specific startup logic.
     */
    protected void doStartup() {
    }

    protected void doShutdown() {
    }

    protected abstract Optional<Path> getRiotClientServicesExecutablePath();

    public CompletableFuture<Void> startRiotClientServices(RiotClientConnectionParameters parameters) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executorService.submit(() -> {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(
                    rcsPath.toAbsolutePath().toString(),
                    "--headless",
                    "--remoting-auth-token=" + parameters.getAuthSecret(),
                    "--app-port=" + parameters.getPort()
            );
            log.info("Starting RCS with command {}", pb.command());
            try {
                doStartProcess(pb);
            } catch (IOException | IllegalStateException e) {
                future.completeExceptionally(e);
            }
            future.complete(null);
        });
        return future;
    }

    private synchronized void doStartProcess(ProcessBuilder pb) throws IllegalStateException, IOException {
        final Process prevProcess = currentRCSProcess.get();
        if (prevProcess != null && prevProcess.isAlive()) {
            throw new IllegalStateException("Riot Client Services is already running");
        }

        log.info("No previous process found, starting new one");
        Process process = pb.start();
        currentRCSProcess.set(process);
        log.info("Started Riot Client Services with PID: {}", process.pid());
    }

    public CompletableFuture<Void> killRiotClientServices() {
        return getExecutableName(ServiceConfig.ProcessServiceConfig.OSExecutableMappings::getRiotClientServices)
                .thenCompose(executableIdentifier -> {
                    CompletableFuture<?>[] futures = ProcessHandle.allProcesses()
                            .filter(processHandle -> processHandle.info().command().map(command -> command.endsWith(executableIdentifier)).orElse(false))
                            .limit(1)
                            .map(this::killProcess)
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                });
    }

    public CompletableFuture<Void> killGameProcess(SupportedGame game) {
        return getExecutableName(mappings -> mappings.getGameExecutables().get(game))
                .thenCompose(executableIdentifier -> {
                    CompletableFuture<?>[] futures = ProcessHandle.allProcesses()
                            .filter(processHandle -> processHandle.info().command().map(command -> command.endsWith(executableIdentifier)).orElse(false))
                            .map(this::killProcess)
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                });
    }

    public CompletableFuture<Void> killRiotClientProcess() {
        return getExecutableName(ServiceConfig.ProcessServiceConfig.OSExecutableMappings::getRiotClient)
                .thenCompose(executableIdentifier -> {
                    CompletableFuture<?>[] futures = ProcessHandle.allProcesses()
                            .filter(processHandle -> processHandle.info().command().map(command -> command.endsWith(executableIdentifier)).orElse(false))
                            .sorted(Comparator.comparingLong(ProcessHandle::pid))
                            .limit(1)
                            .map(this::killProcess)
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                });
    }

    public List<SupportedGame> getSupportedGames() {
        return Arrays.stream(SupportedGame.values())
                .filter(supportedGame -> {
                    final String executableName = getOsExecutableNames().getGameExecutables().get(supportedGame);
                    return executableName != null && !executableName.isEmpty();
                })
                .toList();
    }

    protected CompletableFuture<String> getExecutableName(Function<ServiceConfig.ProcessServiceConfig.OSExecutableMappings, String> accessorFunction) {
        return CompletableFuture.supplyAsync(() -> {
            final String executableName = accessorFunction.apply(this.config.getExecutables().get(operatingSystem));
            if (executableName == null || executableName.isBlank()) {
                throw new UnsupportedOperationException("Executable is not supported on this OS");
            }
            return executableName;
        }, executorService);
    }

    protected CompletableFuture<Void> killProcess(ProcessHandle processHandle) {
        return processHandle.onExit().orTimeout(500, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        log.warn("Process {} timed out, killing it forcibly", processHandle.pid());
                        processHandle.destroyForcibly();
                        return processHandle;
                    }
                    throw new RuntimeException("Failed to kill process " + processHandle.pid(), ex);
                })
                .thenComposeAsync(processHandle1 -> processHandle
                        .onExit()
                        .orTimeout(2, TimeUnit.SECONDS)
                        .thenRun(() -> {
                            log.info("Process {} killed successfully", processHandle1.pid());
                        }));
    }
}
