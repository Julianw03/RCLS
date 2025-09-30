package com.julianw03.rcls.service.base.process;

import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.config.mappings.ProcessServiceConfig;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.process.ProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessServiceTest {
    private static final Duration FAIL_FAST_ACCEPTABLE_DURATION = Duration.of(5, ChronoUnit.SECONDS);

    @Mock
    PathProvider pathProvider;

    @Mock
    ProcessServiceConfig processServiceConfig;

    @Test
    void testProcessServiceNormalDestroyWorks() {
        CompletableFuture<ProcessHandle> onExitFuture = new CompletableFuture<>();

        ProcessHandle processHandle = mock(ProcessHandle.class);

        when(processHandle.destroy()).thenAnswer(invocation -> {
            onExitFuture.complete(processHandle); // Simulate process exit
            return true;
        });
        when(processHandle.onExit()).thenReturn(onExitFuture);

        ProcessService processService = setupProcessService();

        CompletableFuture<Void> result = processService.killProcess(processHandle);

        assertDoesNotThrow(() -> result.get(2, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(result.isDone());
    }

    @Test
    void testProcessServiceFailsReasonablyFast() {
        CompletableFuture<ProcessHandle> onExitFuture = new CompletableFuture<>();

        ProcessHandle processHandle = mock(ProcessHandle.class);

        when(processHandle.destroy()).thenReturn(true);
        when(processHandle.destroyForcibly()).thenReturn(true);

        when(processHandle.onExit()).thenAnswer(ignored -> {
            /**
             * With this we ensure that each call to onExit returns a new instance of {@link CompletableFuture}.
             */
            return onExitFuture.thenApply(Function.identity());
        });

        ProcessService processService = setupProcessService();

        assertTimeoutPreemptively(
                FAIL_FAST_ACCEPTABLE_DURATION,
                () -> {
                    Throwable exception = assertThrowsExactly(
                            ExecutionException.class,
                            () -> processService.killProcess(processHandle).get(),
                            "ProcessService should fail with an Execution-Exception if the process does not exit in time."
                    );
                    assertInstanceOf(TimeoutException.class, exception.getCause(), "Cause should be a TimeoutException");
                },
                "ProcessService should fail within a reasonable time frame. -> " + FAIL_FAST_ACCEPTABLE_DURATION
        );
    }

    @Test
    void testProcessServiceForcibleDestroyWorks() {
        CompletableFuture<ProcessHandle> onExitFuture = new CompletableFuture<>();

        ProcessHandle processHandle = mock(ProcessHandle.class);

        when(processHandle.destroy()).thenReturn(true);
        when(processHandle.destroyForcibly()).thenAnswer(invocation -> {
            onExitFuture.complete(processHandle);
            return true;
        });

        when(processHandle.onExit()).thenAnswer(ignored -> {
            /**
             * With this we ensure that each call to onExit returns a new instance of {@link CompletableFuture}.
             */
            return onExitFuture.thenApply(Function.identity());
        });

        ProcessService processService = setupProcessService();

        CompletableFuture<Void> result = processService.killProcess(processHandle);

        assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        assertTrue(result.isDone());
    }

    @Test
    void testSupportedGameKillWorks() {
        final Map<SupportedGame, String> executableNames = Arrays.stream(SupportedGame.values())
                .collect(
                        HashMap::new,
                        (acc, current) -> acc.put(current, current.getDisplayName()),
                        Map::putAll
                );


        PathProviderConfig.PathEntries mockEntries = new PathProviderConfig.PathEntries();
        PathProviderConfig.PathEntries.Executables mockExecutables = new PathProviderConfig.PathEntries.Executables();

        mockEntries.setExecutables(mockExecutables);
        mockExecutables.setGameExecutables(executableNames);

        when(pathProvider.get()).thenReturn(mockEntries);

        Arrays.stream(SupportedGame.values()).forEach(this::testSingleGame);
    }

    private void testSingleGame(SupportedGame game) {
        ProcessHandle mockGameProcessHandle = mock(ProcessHandle.class);
        ProcessHandle.Info mockInfo = mock(ProcessHandle.Info.class);

        doReturn(Optional.ofNullable(game.getDisplayName())).when(mockInfo).command();

        when(mockGameProcessHandle.info()).thenReturn(mockInfo);
        when(mockGameProcessHandle.destroy()).thenReturn(true);
        when(mockGameProcessHandle.pid()).thenReturn(0L);
        when(mockGameProcessHandle.onExit()).thenReturn(CompletableFuture.completedFuture(mockGameProcessHandle));

        ProcessService processService = setupProcessService(mockGameProcessHandle);

        assertDoesNotThrow(() -> processService.killGameProcess(game).get());
    }

    @Test
    void testUnsupportedGameThrows() {

        ProcessHandle mockProcessHandle = mock(ProcessHandle.class);

        ProcessService processService = setupProcessService(mockProcessHandle);

        PathProviderConfig.PathEntries mockEntries = new PathProviderConfig.PathEntries();
        PathProviderConfig.PathEntries.Executables mockExecutables = new PathProviderConfig.PathEntries.Executables();

        mockEntries.setExecutables(mockExecutables);
        mockExecutables.setGameExecutables(Collections.emptyMap());

        when(pathProvider.get()).thenReturn(mockEntries);

        ExecutionException exception = assertThrowsExactly(ExecutionException.class, () -> processService.killGameProcess(null).get());
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }


    private ProcessService setupProcessService(ProcessHandle... processHandles) {
        Stream<ProcessHandle> processHandleStream = Arrays.stream(processHandles);

        assertFalse(processHandleStream.anyMatch(Objects::isNull), "ProcessHandles should not be null");

        return new ProcessService(
                pathProvider,
                () -> Arrays.stream(processHandles),
                processServiceConfig
        );
    }
}
