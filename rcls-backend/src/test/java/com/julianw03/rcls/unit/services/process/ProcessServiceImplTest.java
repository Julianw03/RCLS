package com.julianw03.rcls.unit.services.process;

import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.config.mappings.ProcessServiceConfig;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.process.ProcessServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessServiceImplTest {
    private static final Duration FAIL_FAST_ACCEPTABLE_DURATION = Duration.of(5, ChronoUnit.SECONDS);

    @Mock
    PathProvider pathProvider;

    @Mock
    ProcessServiceConfig processServiceConfig;

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

        assertDoesNotThrow(() -> processService.killGameProcess(game));
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

        assertThrowsExactly( UnsupportedOperationException.class, () -> processService.killGameProcess(null));
    }

    @Test
    void testNoGameProcessRunningDoesThrow() {
        ProcessService processService = setupProcessService();
        final Map<SupportedGame, String> executableNames = Arrays.stream(SupportedGame.values())
                                                                 .collect(
                                                                         HashMap::new,
                                                                         (acc, current) -> acc.put(current, current.getDisplayName()),
                                                                         Map::putAll
                                                                 );


        PathProviderConfig.PathEntries mockEntries = new PathProviderConfig.PathEntries();
        PathProviderConfig.PathEntries.Executables mockExecutables = new PathProviderConfig.PathEntries.Executables();
        mockExecutables.setGameExecutables(executableNames);

        mockEntries.setExecutables(mockExecutables);

        when(pathProvider.get()).thenReturn(mockEntries);

        assertThrowsExactly(NoSuchProcessException.class, () -> processService.killGameProcess(SupportedGame.LEAGUE_OF_LEGENDS));
    }


    private ProcessServiceImpl setupProcessService(ProcessHandle... processHandles) {
        Stream<ProcessHandle> processHandleStream = Arrays.stream(processHandles);

        assertFalse(processHandleStream.anyMatch(Objects::isNull), "ProcessHandles should not be null");

        return new ProcessServiceImpl(
                pathProvider,
                () -> Arrays.stream(processHandles),
                processServiceConfig
        );
    }
}
