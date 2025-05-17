package com.julianw03.rcls.service;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.riotclient.RiotClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RiotClientServiceImplTest {

    @Mock
    private ProcessService                 processService;  // Mock external process service
    @Mock
    private HttpClient                     httpClient;         // Mock HTTP client
    @Mock
    private RiotClientConnectionParameters connectionParameters;  // Mock connection parameters

    @InjectMocks
    private RiotClientServiceImpl riotClientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        riotClientService = new RiotClientServiceImpl(processService);
    }

    @Test
    void testConnect_whenAllProcessCallsSucceed() {
        riotClientService = spy(riotClientService);

        when(processService.killGameProcess(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcess()).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientServices()).thenReturn(CompletableFuture.completedFuture(null));

        when(processService.startRiotClientServices(any())).thenReturn(CompletableFuture.completedFuture(null));

        when(riotClientService.awaitRestReady(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(riotClientService.awaitWebsocketConnection(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> riotClientService.connect());
    }

    @Test
    void testConnect_whenRiotClientServicesKillDoesntSucceed() {

        when(processService.killGameProcess(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcess()).thenReturn(CompletableFuture.completedFuture(null));

        when(processService.killRiotClientServices()).thenReturn(CompletableFuture.failedFuture(new APIException("Mock error")));

        riotClientService = spy(riotClientService);
        assertThrows(APIException.class, () -> {
            riotClientService.connect();
        });
    }
}
