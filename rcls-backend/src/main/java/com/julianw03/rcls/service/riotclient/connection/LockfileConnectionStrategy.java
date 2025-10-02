package com.julianw03.rcls.service.riotclient.connection;

import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.providers.paths.PathProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class tries to get the {@link RiotClientConnectionParameters} via the Lockfile that is
 * created once the Riot Client (UX - Process) starts up.
 * This strategy expects the UX Client to be running
 * */
@Slf4j
public class LockfileConnectionStrategy implements RiotClientConnectionStrategy {
    private final PathProvider pathProvider;

    public LockfileConnectionStrategy(
            PathProvider pathProvider
    ) {
        this.pathProvider = pathProvider;
    }

    @Override
    public RiotClientConnectionParameters connect() throws Exception {

        Path lockfilePath = Paths.get(pathProvider.get().getRiotClientLockFileLocation());
        if (!lockfilePath.toFile().exists()) {
            throw new UnsupportedOperationException("Lockfile not found at " + lockfilePath);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(lockfilePath.toFile()))) {
            StringBuilder lockfileContent = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                lockfileContent.append((char) character);
            }

            String[] parts = lockfileContent.toString().split(":");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid lockfile format");
            }

            String identifier = parts[0];
            String pid = parts[1];
            String port = parts[2];
            String secret = parts[3];
            String protocol = parts[4];

            if (!"Riot Client".equalsIgnoreCase(identifier)) {
                throw new IllegalArgumentException("Invalid lockfile identifier: " + identifier);
            }
            return new RiotClientConnectionParameters(
                    secret,
                    Integer.parseInt(port)
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public void disconnect() {
    }
}
