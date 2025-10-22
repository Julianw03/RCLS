package com.julianw03.rcls.service.base.riotclient.connection;

import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.process.ProcessServiceImpl;
import com.julianw03.rcls.service.riotclient.connection.ProcessTakeoverConnectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessTakeoverStrategyTest {
    @Mock
    private ProcessServiceImpl processService;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private ProcessTakeoverConnectionStrategy processTakeoverConnectionStrategy;

    @BeforeEach
    public void setUp() {
        processTakeoverConnectionStrategy = new ProcessTakeoverConnectionStrategy(processService);
    }

    @Test
    void connect_should_work() {
        when(processService.killGameProcessAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcessAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientServicesAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(processService.startRiotClientServicesAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        RiotClientConnectionParameters connectionParameters = assertDoesNotThrow(processTakeoverConnectionStrategy::connect);

        ArgumentCaptor<SupportedGame> captor = ArgumentCaptor.forClass(SupportedGame.class);
        verify(processService, times(SupportedGame.values().length)).killGameProcessAsync(captor.capture());

        List<SupportedGame> captured = captor.getAllValues();
        // Casting to set is necessary as sets don't care about the order of the elments for equality
        assertEquals(Set.of(SupportedGame.values()), new HashSet<>(captured));

        assertNotNull(connectionParameters);
        assertNotNull(connectionParameters.getAuthSecret());
        assertNotNull(connectionParameters.getPort());
    }

    @Test
    void unsupportedGamesShouldNotAffectSuccess() {
        when(processService.killGameProcessAsync(any())).thenReturn(CompletableFuture.runAsync(() -> {
            throw new UnsupportedOperationException();
        }, executorService));
        when(processService.killRiotClientProcessAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientServicesAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(processService.startRiotClientServicesAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        RiotClientConnectionParameters connectionParameters = assertDoesNotThrow(processTakeoverConnectionStrategy::connect);
        assertNotNull(connectionParameters);
        assertNotNull(connectionParameters.getAuthSecret());
        assertNotNull(connectionParameters.getPort());
    }

    @Test
    void throwsWhenClientProcessKillFails() {
        when(processService.killGameProcessAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcessAsync()).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

        assertThrowsExactly(ExecutionException.class, processTakeoverConnectionStrategy::connect);
    }

    @Test
    void throwsWhenRiotClientServiceKillFails() {
        when(processService.killGameProcessAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcessAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientServicesAsync()).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

        assertThrowsExactly(ExecutionException.class, processTakeoverConnectionStrategy::connect);
    }
}
