package com.julianw03.rcls.service.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.FutureUtils;
import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.config.mappings.ProcessServiceConfig;
import com.julianw03.rcls.controller.FailFastException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.BaseService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
public class ProcessServiceImpl extends BaseService implements ProcessService {

    protected final ProcessServiceConfig            config;
    protected final PathProvider                    pathProvider;
    protected final Supplier<Stream<ProcessHandle>> processHandleSupplier;
    protected final ScheduledExecutorService        executorService;
    protected final AtomicReference<Process>        currentRCSProcess;
    protected       Path                            rcsPath;
    private final   ObjectMapper                    mapper;

    public ProcessServiceImpl(
            PathProvider pathProvider,
            Supplier<Stream<ProcessHandle>> processHandleSupplier,
            ProcessServiceConfig config
    ) {
        this.config = config;
        this.processHandleSupplier = processHandleSupplier;
        this.pathProvider = pathProvider;
        this.executorService = Executors.newScheduledThreadPool(10);
        this.mapper = new ObjectMapper();
        this.currentRCSProcess = new AtomicReference<>();
    }

    protected PathProviderConfig.PathEntries.Executables getOsExecutableNames() {
        return pathProvider.get()
                           .getExecutables();
    }

    @Override
    public final void startup() {
        log.info("Startup called");
        long start = System.currentTimeMillis();
        rcsPath = getRiotClientServicesExecutablePath().orElseThrow(() -> new IllegalStateException("Riot Client Services executable path could not be found"));
        log.info(
                "Riot Client Services executable path: {}",
                rcsPath
        );
        doStartup();
        long end = System.currentTimeMillis();
        log.info(
                "Startup succeeded after {}ms",
                end - start
        );
    }

    @Override
    public final void shutdown() {
        long start = System.currentTimeMillis();
        try {
            doShutdown();
        } catch (Exception e) {
            log.error(
                    "doShutdown threw Exception, will proceed with rest of shutdown flow",
                    e
            );
        }
        Optional.ofNullable(currentRCSProcess.getAndSet(null))
                .ifPresent(Process::destroyForcibly);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(
                    800,
                    TimeUnit.MILLISECONDS
            )) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error(
                    "Graceful shutdown failed",
                    e
            );
            executorService.shutdownNow();
        }
        executorService.shutdown();
        long end = System.currentTimeMillis();
        log.info(
                "Shutdown succeeded after {}ms",
                (end - start)
        );
    }

    /**
     * May be used by subclasses to perform any specific startup logic.
     */
    protected void doStartup() {
    }

    protected void doShutdown() {
    }

    private Optional<Path> getRiotClientServicesExecutablePath() {
        final PathProviderConfig.PathEntries pathEntries = pathProvider.get();

        Path path = Path.of(
                pathEntries.getProgramFilesPath(),
                pathEntries.getRiotGamesFolderName(),
                pathEntries.getRiotClientInstallsFile()
        );

        File riotClientInstallsFile = path.toFile();
        if (!riotClientInstallsFile.exists() && !riotClientInstallsFile.isFile()) {
            log.error(
                    "Riot Client Installs file does not exist or is not a File: {}",
                    path
            );
            return Optional.empty();
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode node = mapper.readTree(new InputStreamReader(is));
            if (node.isEmpty() || !node.isObject()) {
                log.error("Riot Client Installs file is empty or not a JSON object");
                return Optional.empty();
            }

            ObjectNode objectNode = (ObjectNode) node;
            String[] useKeys = new String[]{"rc_live", "rc_default"};
            JsonNode useNode = null;
            for (String lookupKey : useKeys) {
                if ((useNode = objectNode.get(lookupKey)) != null && !useNode.isNull()) break;
                log.info(
                        "Lookup for key {} failed",
                        lookupKey
                );
            }

            if (useNode == null) {
                log.error("Unable to get RCS Path, all lookups failed");
                return Optional.empty();
            }

            String stringRCSPath = useNode.asText();
            if (stringRCSPath == null || stringRCSPath.isEmpty()) {
                log.error("The Riot Client Services path is empty");
                return Optional.empty();
            }

            Path rcsPath = Paths.get(stringRCSPath);
            File rcsFile = rcsPath.toFile();
            if (!rcsFile.exists() || !rcsFile.isFile()) {
                log.error("The Riot Client Services file does not exist");
                return Optional.empty();
            }

            return Optional.of(rcsPath);
        } catch (IOException e) {
            log.error(
                    "Failed to read Riot Client Installs file",
                    e
            );
            return Optional.empty();
        }
    }

    public CompletableFuture<Void> startRiotClientServicesAsync(RiotClientConnectionParameters parameters) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executorService.submit(() -> {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(
                    rcsPath.toAbsolutePath()
                           .toString(),
                    "--headless",
                    "--remoting-auth-token=" + parameters.getAuthSecret(),
                    "--app-port=" + parameters.getPort()
            );
            log.info(
                    "Starting RCS with command {}",
                    pb.command()
            );
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
        log.info(
                "Started Riot Client Services with PID: {}",
                process.pid()
        );
    }

    @Override
    public void startRiotClientServices(RiotClientConnectionParameters parameters) throws FailFastException {
        try {
            startRiotClientServicesAsync(parameters).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FailFastException failFastException) {
                throw failFastException;
            }
            throw new RuntimeException(
                    "Unexpected exception occurred",
                    e.getCause()
            );
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
    }

    @Override
    public void killRiotClientServices() throws FailFastException, NoSuchProcessException {
        try {
            killRiotClientServicesAsync().get();
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case FailFastException failFastException:
                    throw failFastException;
                case NoSuchProcessException noSuchProcessException:
                    throw noSuchProcessException;
                default:
                    throw new IllegalStateException("Unexpected value: " + e.getCause());
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();

        }

    }

    @Override
    public void killRiotClientProcess() throws NoSuchProcessException, FailFastException {
        try {
            killRiotClientProcessAsync().get();
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case NoSuchProcessException noSuchProcessException:
                    throw noSuchProcessException;
                case FailFastException failFastException:
                    throw failFastException;
                default:
                    throw new RuntimeException(
                            "Unexpected exception occurred",
                            e.getCause()
                    );
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
    }

    public CompletableFuture<Void> killRiotClientServicesAsync() {
        return getExecutableName((PathProviderConfig.PathEntries.Executables::getRiotClientServices))
                .thenComposeAsync(
                        FutureUtils.wrapIntoExceptionFunction(executableIdentifier -> {
                            List<CompletableFuture<Void>> futures = processHandleSupplier.get()
                                                                                         .filter(processHandle -> processHandle.info()
                                                                                                                               .command()
                                                                                                                               .map(command -> command.endsWith(executableIdentifier))
                                                                                                                               .orElse(false))
                                                                                         .limit(1)
                                                                                         .map(this::killProcess)
                                                                                         .toList();
                            if (futures.isEmpty()) {
                                CompletableFuture.failedFuture(new NoSuchProcessException("No running Riot Client Services process found"));
                            }

                            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        }),
                        executorService
                );
    }

    public CompletableFuture<Void> killGameProcessAsync(SupportedGame game) {
        return getExecutableName(mappings -> mappings.getGameExecutables()
                                                     .get(game))
                .thenComposeAsync(
                        FutureUtils.wrapIntoExceptionFunction(executableIdentifier -> {
                            List<CompletableFuture<Void>> futures = processHandleSupplier.get()
                                                                                         .filter(processHandle -> processHandle.info()
                                                                                                                               .command()
                                                                                                                               .map(command -> command.endsWith(executableIdentifier))
                                                                                                                               .orElse(false))
                                                                                         .map(this::killProcess)
                                                                                         .toList();

                            if (futures.isEmpty()) {
                                return CompletableFuture.failedFuture(new NoSuchProcessException(
                                        "No running process found for game: " +
                                        game));
                            }

                            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        }),
                        executorService
                );
    }

    @Override
    public void killGameProcess(SupportedGame game) throws NoSuchProcessException, UnsupportedOperationException, IllegalArgumentException {
        try {
            killGameProcessAsync(game).get();
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case NoSuchProcessException noSuchProcessException:
                    throw noSuchProcessException;
                case UnsupportedOperationException unsupportedOperationException:
                    throw unsupportedOperationException;
                default:
                    throw new RuntimeException(
                            "Unexpected exception occurred",
                            e.getCause()
                    );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> killRiotClientProcessAsync() {
        return getExecutableName(PathProviderConfig.PathEntries.Executables::getRiotClient)
                .thenCompose(executableIdentifier -> {
                    CompletableFuture<?>[] futures = processHandleSupplier.get()
                                                                          .filter(processHandle -> processHandle.info()
                                                                                                                .command()
                                                                                                                .map(command -> command.endsWith(executableIdentifier))
                                                                                                                .orElse(false))
                                                                          .sorted(Comparator.comparingLong(ProcessHandle::pid))
                                                                          .limit(1)
                                                                          .map(this::killProcess)
                                                                          .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                });
    }

    @Override
    public List<SupportedGame> getSupportedGames() {
        return Arrays.stream(SupportedGame.values())
                     .filter(supportedGame -> {
                         final String executableName = getOsExecutableNames().getGameExecutables()
                                                                             .get(supportedGame);
                         return executableName != null && !executableName.isEmpty();
                     })
                     .toList();
    }

    protected CompletableFuture<String> getExecutableName(Function<PathProviderConfig.PathEntries.Executables, String> accessorFunction) {
        return CompletableFuture.supplyAsync(
                () -> {
                    final String executableName = accessorFunction.apply(pathProvider.get()
                                                                                     .getExecutables());
                    if (executableName == null || executableName.isBlank()) {
                        throw new UnsupportedOperationException("Executable is not supported on this OS");
                    }
                    return executableName;
                },
                executorService
        );
    }

    protected CompletableFuture<Void> killProcess(ProcessHandle processHandle) {
        if (processHandle == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ProcessHandle cannot be null"));
        }
        processHandle.destroy();

        return processHandle.onExit()
                            .orTimeout(
                                    500,
                                    TimeUnit.MILLISECONDS
                            )
                            .exceptionally(ex -> {
                                if (ex instanceof TimeoutException) {
                                    log.warn(
                                            "Process {} timed out, killing it forcibly",
                                            processHandle.pid()
                                    );
                                    processHandle.destroyForcibly();
                                    return processHandle;
                                }
                                throw new CompletionException(
                                        "Failed to kill process " + processHandle.pid(),
                                        ex
                                );
                            })
                            .thenCompose(procHandle -> procHandle.onExit()
                                                                 .orTimeout(
                                                                         2,
                                                                         TimeUnit.SECONDS
                                                                 )
                                                                 .thenRun(() -> log.info(
                                                                         "Process {} killed successfully",
                                                                         processHandle.pid()
                                                                 )));
    }
}
