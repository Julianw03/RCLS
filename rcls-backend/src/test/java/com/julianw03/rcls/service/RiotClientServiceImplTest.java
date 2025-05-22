package com.julianw03.rcls.service;

import com.julianw03.rcls.config.mappings.RiotClientServiceConfig;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.riotclient.RiotClientServiceImpl;
import com.julianw03.rcls.service.base.riotclient.connection.ProcessTakeoverConnectionStrategy;
import com.julianw03.rcls.service.base.riotclient.connection.RiotClientConnectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Profile("test")
public class RiotClientServiceImplTest {

    @MockitoBean
    private ProcessService processService;

    @Mock
    private RiotClientConnectionParameters riotClientConnectionParameters;

    @MockitoBean
    private RiotClientConnectionStrategy processTakeoverConnectionStrategy;

    @Autowired
    private RiotClientServiceConfig riotClientServiceConfig;

    @InjectMocks
    private RiotClientServiceImpl riotClientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        riotClientService = new RiotClientServiceImpl(
                processService,
                processTakeoverConnectionStrategy,
                riotClientServiceConfig
        );
    }

    @Test
    void testConnect_whenAllProcessCallsSucceed() throws Exception {
        riotClientService = spy(riotClientService);

        when(processTakeoverConnectionStrategy.connect()).thenReturn(riotClientConnectionParameters);

        when(processService.killGameProcess(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientProcess()).thenReturn(CompletableFuture.completedFuture(null));
        when(processService.killRiotClientServices()).thenReturn(CompletableFuture.completedFuture(null));

        when(processService.startRiotClientServices(any())).thenReturn(CompletableFuture.completedFuture(null));

        when(riotClientService.awaitRestReady(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(riotClientService.awaitWebsocketConnection(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> riotClientService.connect());
    }
}
