package com.julianw03.rcls.service.base.riotclient.connection;

import com.julianw03.rcls.controller.FailFastException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import com.julianw03.rcls.service.process.ProcessService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessTakeoverStrategyTest {
    @Mock
    private ProcessService processService;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private ProcessTakeoverConnectionStrategy processTakeoverConnectionStrategy;

    @BeforeEach
    public void setUp() {
        processTakeoverConnectionStrategy = new ProcessTakeoverConnectionStrategy(processService);
    }

    @Test
    void connect_should_work() {
        ArgumentCaptor<SupportedGame> captor = ArgumentCaptor.forClass(SupportedGame.class);
        final RiotClientConnectionParameters connectionParameters;
        try {
            doNothing().when(processService)
                       .killGameProcess(any());
            doNothing().when(processService)
                       .killRiotClientProcess();
            doNothing().when(processService)
                       .killRiotClientServices();

            doNothing().when(processService)
                       .startRiotClientServices(any());
            connectionParameters = assertDoesNotThrow(processTakeoverConnectionStrategy::connect);
        } catch (Exception e) {
            fail("Method setup should not throw an exception", e);
            return;
        }

        try {
            verify(
                    processService,
                    times(SupportedGame.values().length)
            ).killGameProcess(captor.capture());
        } catch (NoSuchProcessException e) {
            fail("Verification should not throw an exception", e);
        }


        List<SupportedGame> captured = captor.getAllValues();
        // Casting to set is necessary as sets don't care about the order of the elments for equality
        assertEquals(
                Set.of(SupportedGame.values()),
                new HashSet<>(captured)
        );

        assertNotNull(connectionParameters);
        assertNotNull(connectionParameters.getAuthSecret());
        assertNotNull(connectionParameters.getPort());
    }

    @Test
    void unsupportedGamesShouldNotAffectSuccess() {
        assertDoesNotThrow(() -> {
            doThrow(new UnsupportedOperationException()).when(processService)
                                                        .killGameProcess(any());
            doNothing().when(processService)
                       .killRiotClientProcess();
            doNothing().when(processService)
                       .killRiotClientServices();

            doNothing().when(processService)
                       .startRiotClientServices(any());
            RiotClientConnectionParameters connectionParameters = assertDoesNotThrow(processTakeoverConnectionStrategy::connect);
            assertNotNull(connectionParameters);
            assertNotNull(connectionParameters.getAuthSecret());
            assertNotNull(connectionParameters.getPort());
        });
    }

    @Test
    void throwsWhenClientProcessKillFails() {
        try {
            doNothing().when(processService).killGameProcess(any());
            doThrow(new FailFastException()).when(processService).killRiotClientProcess();
        } catch (NoSuchProcessException e) {
            fail("Method setup should not throw an exception", e);
            return;
        }

        assertThrowsExactly(ExecutionException.class, processTakeoverConnectionStrategy::connect);
    }

    @Test
    void throwsWhenRiotClientServiceKillFails() {
        try {
            doNothing().when(processService).killGameProcess(any());
            doNothing().when(processService).killRiotClientProcess();
            doThrow(new FailFastException()).when(processService).killRiotClientServices();
        } catch (NoSuchProcessException e) {
            fail("Method setup should not throw an exception", e);
            return;
        }

        assertThrowsExactly(ExecutionException.class, processTakeoverConnectionStrategy::connect);
    }
}
