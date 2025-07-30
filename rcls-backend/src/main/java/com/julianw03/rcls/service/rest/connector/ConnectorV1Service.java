package com.julianw03.rcls.service.rest.connector;

import java.util.concurrent.ExecutionException;

public interface ConnectorV1Service {
    /**
     * Attempts to establish a connection to the Riot Client.
     *
     * @throws IllegalStateException When the connection is already established or is in the process of being established.
     * @throws ExecutionException    When an error occurs during the connection process.
     */
    RiotClientConnectionParametersDTO connect() throws IllegalStateException, ExecutionException;

    /**
     * Retrieves the current connection parameters of the Riot Client.
     *
     * @return The current connection parameters.
     * @throws IllegalStateException When the connection is not established or has been disconnected.
     */
    RiotClientConnectionParametersDTO getConnectionParameters() throws IllegalStateException;

    /**
     * Attempts to disconnect from the Riot Client.
     * @throws IllegalStateException When the connection is not established or has already been disconnected.
     * @throws ExecutionException    When an error occurs during the disconnection process.
     */
    void disconnect() throws IllegalStateException, ExecutionException;
}
