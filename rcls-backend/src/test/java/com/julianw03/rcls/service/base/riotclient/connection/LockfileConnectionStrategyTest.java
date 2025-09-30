package com.julianw03.rcls.service.base.riotclient.connection;

import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.riotclient.connection.LockfileConnectionStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockfileConnectionStrategyTest {

    @TempDir
    Path tempDir;

    @Mock
    private PathProvider pathProvider;

    private LockfileConnectionStrategy strategy;

    private final RiotClientConnectionParameters exampleParameters = new RiotClientConnectionParameters("TEST", 1234);

    @BeforeEach
    void setUp() {
        strategy = new LockfileConnectionStrategy(pathProvider);
    }

    @Test
    void connect_readsCorrectValuesFromLockfile() {
        setupFileContents(String.format("Riot Client:0:%d:%s:https",
                exampleParameters.getPort(), exampleParameters.getAuthSecret()));


        RiotClientConnectionParameters params = assertDoesNotThrow(strategy::connect);
        assertEquals(exampleParameters.getPort(), params.getPort());
        assertEquals(exampleParameters.getAuthSecret(), params.getAuthSecret());
    }

    @Test
    void connect_failsWhenInvalidIdentifierIsRead() {
        setupFileContents(String.format("NOT RIOT CLIENT:0:%d:%s:https",
                exampleParameters.getPort(), exampleParameters.getAuthSecret()));


        assertThrowsExactly(IllegalArgumentException.class, strategy::connect);
    }

    @Test
    void connect_throwsWhenLockfileIsInInvalidFormat() {
        setupFileContents("::");

        assertThrowsExactly(IllegalArgumentException.class, strategy::connect);
    }

   @Test
   void connect_throwsWhenFileIsNotFound() {
       PathProviderConfig.PathEntries entries = new PathProviderConfig.PathEntries();
       entries.setRiotClientLockFileLocation("");
       when(pathProvider.get()).thenReturn(entries);

       assertThrows(Exception.class, strategy::connect);
   }

    private void setupFileContents(String contents) {
        Path lockfile = tempDir.resolve("lockfile");
        PathProviderConfig.PathEntries entries = new PathProviderConfig.PathEntries();
        entries.setRiotClientLockFileLocation(lockfile.toString());
        when(pathProvider.get()).thenReturn(entries);

        assertDoesNotThrow(() -> {
            Files.write(lockfile, contents.getBytes());
        });
    }

}