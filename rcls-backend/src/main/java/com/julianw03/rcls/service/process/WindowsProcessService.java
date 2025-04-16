package com.julianw03.rcls.service.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.config.ServiceConfig;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WindowsProcessService extends ProcessService {

    private static final Logger log = LoggerFactory.getLogger(WindowsProcessService.class);

    private static class ProcessResult {
        @Getter
        private ResultStatus status;
        private String       stdOut;
        private String       stdErr;
        private Throwable    throwable;

        public static ProcessResult ofSuccess(String stdOut) {
            if (stdOut == null) throw new IllegalArgumentException("StdOut may not be null");
            ProcessResult result = new ProcessResult();
            result.status = ResultStatus.SUCCESS;
            result.stdOut = stdOut;
            return result;
        }

        public static ProcessResult ofError(String stdError) {
            if (stdError == null) throw new IllegalArgumentException("StdErr may not be null");
            ProcessResult result = new ProcessResult();
            result.status = ResultStatus.STD_ERROR_NOT_EMPTY;
            result.stdErr = stdError;
            return result;
        }

        public static ProcessResult ofEmptyStdOut() {
            ProcessResult result = new ProcessResult();
            result.status = ResultStatus.STD_OUT_EMPTY;
            return result;
        }

        public static ProcessResult ofThrowable(Throwable t) {
            if (t == null) throw new IllegalArgumentException("Throwable may not be null");
            ProcessResult result = new ProcessResult();
            result.status = ResultStatus.THREW_ERROR;
            result.throwable = t;
            return result;
        }

        private ProcessResult() {
        }

        public String getStdOut() {
            if (this.status != ResultStatus.SUCCESS) throw new IllegalStateException();
            return this.stdOut;
        }

        public String getStdErr() {
            if (this.status != ResultStatus.STD_ERROR_NOT_EMPTY) throw new IllegalStateException();
            return this.stdErr;
        }

        public Throwable getThrowable() {
            if (this.status != ResultStatus.THREW_ERROR) throw new IllegalStateException();
            return this.throwable;
        }
    }

    public enum ResultStatus {
        STD_ERROR_NOT_EMPTY,
        STD_OUT_EMPTY,
        THREW_ERROR,
        SUCCESS
    }

    private final ScheduledExecutorService executorService;
    private final ObjectMapper             mapper;
    private       Path                     rcsPath;
    private       Process                  currentRcsProcess;

    public WindowsProcessService(
            ServiceConfig.ProcessServiceConfig config
    ) {
        super(
                SupportedOperatingSystem.WINDOWS,
                config
        );
        mapper = new ObjectMapper();
        executorService = Executors.newScheduledThreadPool(10);
    }

    @Override
    public CompletableFuture<Void> startRiotClientServices(RiotClientConnectionParameters parameters) {
        if (parameters == null) throw new IllegalArgumentException();
        final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            if (currentRcsProcess != null && currentRcsProcess.isAlive()) {
                log.info("Previous process exists, will try to kill it before starting a new RCS instance");
                try {
                    currentRcsProcess.destroyForcibly().waitFor();
                } catch (Exception e) {
                    completableFuture.completeExceptionally(new IllegalStateException());
                    return;
                }
            }
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("\"" + rcsPath.toAbsolutePath() + "\"", "--show-swagger", "--show-dev-tools", "--riotclient-dev-mode", "--show-dev-tools-all","--headless", "--remoting-auth-token=" + parameters.getAuthSecret(), "--app-port=" + parameters.getPort());
            log.info("Starting RCS with command {}", pb.command());
            try {
                currentRcsProcess = pb.start();
            } catch (Exception e) {
            }
            completableFuture.complete(null);
        });

        return completableFuture.thenCompose((ignored) -> awaitProcessStart(getOsExecutableNames().getRiotClientServices())).thenApply((val) -> null);
    }

    @Override
    public CompletableFuture<Void> killRiotClientServices() {
        return killProcessViaExecutableName(getOsExecutableNames().getRiotClientServices(), (stdOut) -> {
            final JsonNode node;
            try {
                node = mapper.readTree(stdOut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!node.isObject()) {
                throw new IllegalArgumentException("Expected JsonObject");
            }

            ObjectNode objectNode = (ObjectNode) node;
            JsonNode processIdNode = objectNode.get("ProcessId");
            if (processIdNode == null || processIdNode.isNull()) {
                throw new IllegalArgumentException("No field with ProcessId found");
            }
            return processIdNode.asLong();
        });
    }

    @Override
    public CompletableFuture<Void> killGameProcess(SupportedGame game) {
        if (game == null) throw new IllegalArgumentException();
        final String executableName = getOsExecutableNames().getGameExecutables().get(game);
        return killProcessViaExecutableName(executableName, (stdOut) -> {
            final JsonNode node;
            try {
                node = mapper.readTree(stdOut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!node.isObject()) {
                throw new IllegalArgumentException("Expected JsonObject");
            }

            ObjectNode objectNode = (ObjectNode) node;
            JsonNode processIdNode = objectNode.get("ProcessId");
            if (processIdNode == null || processIdNode.isNull()) {
                throw new IllegalArgumentException("No field with ProcessId found");
            }
            return processIdNode.asLong();
        });
    }

    @Override
    public CompletableFuture<Void> killRiotClientProcess() {
        return killProcessViaExecutableName(getOsExecutableNames().getRiotClient(), (stdOut -> {
            final JsonNode node;
            try {
                node = mapper.readTree(stdOut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (node.isArray()) {
                /*TODO: Build a better mechanism.
                    This mechanism relies on the Precondition that Parent PID < Children PID for all children PID.
                    This might not always hold true.
                */
                ArrayNode arrNode = (ArrayNode) node;
                Optional<Long> optParentProcessId = StreamSupport.stream(arrNode.spliterator(), false)
                        .filter(JsonNode::isObject)
                        .map(jsonNode -> {
                            ObjectNode objectNode = (ObjectNode) jsonNode;
                            JsonNode valueNode = objectNode.get("ProcessId");
                            if (valueNode == null || valueNode.isNull()) {
                                return null;
                            }
                            return valueNode.longValue();
                        })
                        .filter(Objects::nonNull)
                        .min(Long::compareTo);
                return optParentProcessId.orElseThrow();
            } else if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                JsonNode valueNode = objectNode.get("ProcessId");
                if (valueNode == null || valueNode.isNull()) {
                    throw new IllegalArgumentException("some ");
                }
                return valueNode.asLong();
            } else throw new IllegalArgumentException("some 2");
        }));
    }


    private CompletableFuture<Void> killProcessViaExecutableName(String executableName, Function<String, Long> processIdProvider) {
        final CompletableFuture<Long> future = new CompletableFuture<>();
        executorService.submit(() -> {
            ProcessResult result = getProcessIdForExecutable(executableName);
            if (result.getStatus() == ResultStatus.STD_OUT_EMPTY) {
                //The Process does not exist, we will interpret this as a success
                future.complete(null);
                return;
            }
            if (result.getStatus() != ResultStatus.SUCCESS) {
                future.completeExceptionally(new IllegalStateException("Kill process failed with code " + result.getStatus()));
                return;
            }
            try {
                long processId = processIdProvider.apply(result.getStdOut());
                killProcessWithId(processId);
                future.complete(processId);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future.thenCompose(this::awaitProcessKill);
    }

    private CompletableFuture<Void> awaitProcessKill(Supplier<ProcessResult> processResultSupplier) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final Runnable awaitProcessKillTask = new Runnable() {
            int attemptCount = 0;

            @Override
            public void run() {
                if (attemptCount >= config.getConnectionInit().getProcessSearchAttempts()) {
                    future.completeExceptionally(new TimeoutException("Process did not terminate within the expected time"));
                    return;
                }

                ProcessResult result = processResultSupplier.get();
                if (Objects.requireNonNull(result.getStatus()) == ResultStatus.STD_OUT_EMPTY) {
                    log.info("Process could not be found with provided Result Supplier, assumed terminated");
                    future.complete(null);
                    return;
                }
                attemptCount++;
                executorService.schedule(this, config.getConnectionInit().getProcessSearchDelayMs(), TimeUnit.MILLISECONDS);
            }
        };

        executorService.submit(awaitProcessKillTask);
        return future;
    }

    private CompletableFuture<Void> awaitProcessKill(Long processId) {
        return awaitProcessKill(() -> checkProcessAlive(processId));
    }

    private CompletableFuture<Long> awaitProcessStart(String executableName) {
        final CompletableFuture<Long> future = new CompletableFuture<>();

        final Runnable awaitProcessStartTask = new Runnable() {
            int attemptCount = 0;

            @Override
            public void run() {
                if (attemptCount >= config.getConnectionInit().getProcessSearchAttempts()) {
                    future.completeExceptionally(new TimeoutException("Process did not start within the expected time"));
                    return;
                }

                CompletableFuture<Long> searcher = getProcessIdViaExecutableName(executableName);
                try {
                    Long processId = searcher.get();
                    log.info("Process with name {} found", executableName);
                    future.complete(processId);
                    return;
                } catch (ExecutionException _) {
                    //Current iteration did not find the process, we will continue and schedule another search
                } catch (InterruptedException e) {
                    future.completeExceptionally(e);
                    return;
                }
                attemptCount++;
                executorService.schedule(this, config.getConnectionInit().getProcessSearchDelayMs(), TimeUnit.MILLISECONDS);
            }
        };

        executorService.submit(awaitProcessStartTask);

        return future;
    }

    public CompletableFuture<Long> getProcessIdViaExecutableName(String executableName) {
        CompletableFuture<Long> completableFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            ProcessResult result = getProcessIdForExecutable(executableName);
            ResultStatus status = result.getStatus();
            switch (status) {
                case SUCCESS -> {
                    try {
                        JsonNode node = mapper.reader().readTree(result.getStdOut());
                        if (!node.isObject()) {
                            completableFuture.completeExceptionally(new Throwable());
                            return;
                        }
                        ObjectNode object = (ObjectNode) node;
                        JsonNode some = object.get("ProcessId");
                        if (some == null || some.isNull()) {
                            completableFuture.completeExceptionally(new Throwable());
                            return;
                        }
                        completableFuture.complete(some.asLong());
                    } catch (Exception e) {
                        completableFuture.completeExceptionally(e);
                    }
                }
                default -> completableFuture.completeExceptionally(new IllegalStateException("Not found " + status));
            }
        });
        return completableFuture.orTimeout(config.getConnectionInit().getProcessSearchDurationMaxMs(), TimeUnit.MILLISECONDS);
    }

    @PostConstruct
    @Override
    public void startup() {
        log.info("Startup called");
        log.info("{}", getOsExecutableNames());
        long start = System.currentTimeMillis();

        Optional<Path> optPath = getRiotClientServicesExecutablePath();
        if (optPath.isEmpty()) throw new IllegalStateException();
        this.rcsPath = optPath.get();

        long end = System.currentTimeMillis();
        log.info("Startup succeeded after {}ms", end - start);
    }

    @PreDestroy
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
        Optional.ofNullable(currentRcsProcess).ifPresent(Process::destroyForcibly);
        long end = System.currentTimeMillis();
        log.info("Shutdown succeeded after {}ms", (end - start));
    }

    private ProcessResult getProcessIdForExecutable(String executableName) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("powershell.exe", "-Command", "\"Get-WmiObject -Query \\\"SELECT ProcessId FROM Win32_Process WHERE Name='" + executableName + "'\\\" | ConvertTo-Json\"");
        return handleProcess(processBuilder);
    }

    private ProcessResult checkProcessAlive(Long processId) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("powershell.exe", "-Command", "\"Get-WmiObject -Query \\\"SELECT ProcessId FROM Win32_Process WHERE ProcessId=" + processId + "\\\" | ConvertTo-Json\"");
        return handleProcess(processBuilder);
    }

    private ProcessResult killProcessWithId(Long processId) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("taskkill", "/F", "/PID", processId.toString());
        return handleProcess(processBuilder);
    }

    private ProcessResult handleProcess(ProcessBuilder pb) {
        try {
            Process killProcess = pb.start();
            Optional<String> processIn = Utils.inputStreamToString(killProcess.getInputStream());
            Optional<String> processErr = Utils.inputStreamToString(killProcess.getErrorStream());

            if (processErr.isPresent()) {
                String errString = processErr.get();
                if (!errString.isEmpty()) {
                    return ProcessResult.ofError(errString);
                }
            }

            if (processIn.isEmpty()) {
                return ProcessResult.ofEmptyStdOut();
            }

            String inString = processIn.get();
            if (inString.isEmpty()) return ProcessResult.ofEmptyStdOut();

            return ProcessResult.ofSuccess(inString);
        } catch (Exception e) {
            return ProcessResult.ofThrowable(e);
        }
    }

    private Optional<Path> getRiotClientServicesExecutablePath() {
        String programFiles = System.getenv("ALLUSERSPROFILE");

        if (programFiles == null || programFiles.isEmpty()) {
            log.error("The program files environment variable is empty, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        Path programFilesPath = Paths.get(programFiles);
        Path riotGamesPath = programFilesPath.resolve(config.getSharedComponents().getRiotGamesFolderName());

        File riotGamesFolder = riotGamesPath.toFile();
        if (!riotGamesFolder.exists() || !riotGamesFolder.isDirectory()) {
            log.error("The Riot Games folder does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        Path riotClientInstallsPath = riotGamesPath.resolve(config.getSharedComponents().getRiotClientInstallsFile());
        File riotClientInstallsFile = riotClientInstallsPath.toFile();

        if (!riotClientInstallsFile.exists() || !riotClientInstallsFile.isFile()) {
            log.error("The Riot Client Installs file does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        //Get stream from file
        try (InputStream is = Files.newInputStream(riotClientInstallsFile.toPath())) {
            Optional<String> optJsonString = Utils.inputStreamToString(is);
            final String riotClientInstalls = config.getSharedComponents().getRiotClientInstallsFile();
            if (optJsonString.isEmpty()) {
                log.error("Unable to read the {} file", riotClientInstalls);
                return Optional.empty();
            }

            String jsonString = optJsonString.get();
            if (jsonString.trim().isEmpty()) {
                log.error("{} is empty!", riotClientInstalls);
                return Optional.empty();
            }

            Optional<JsonNode> optJsonNode = Utils.parseJson(mapper, jsonString);
            if (optJsonNode.isEmpty()) {
                log.error("The {} cannot be parsed as Json", riotClientInstalls);
                return Optional.empty();
            }

            JsonNode jsonElement = optJsonNode.get();
            if (!jsonElement.isObject()) {
                log.error("The {} does not contain a Json object", riotClientInstalls);
                return Optional.empty();
            }

            ObjectNode objectNode = (ObjectNode) jsonElement;
            String[] useKeys = new String[]{"rc_live", "rc_default"};
            JsonNode useNode = null;
            for (String lookupKey : useKeys) {
                if ((useNode = objectNode.get(lookupKey)) != null && !useNode.isNull()) break;
                log.info("Lookup for key {} failed", lookupKey);
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

            log.info("The Riot Client Services executable path is: {}", rcsPath);

            return Optional.of(rcsPath);
        } catch (Exception e) {
            log.error("Failed to get RCS Path", e);
        }
        return Optional.empty();
    }
}
