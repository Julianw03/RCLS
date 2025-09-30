package com.julianw03.rcls.service.riotclient.connection;

import com.julianw03.rcls.model.RiotClientConnectionParameters;

/**
 * This interface defines the way that {@link RiotClientConnectionParameters} are retrieved.
 */

public interface RiotClientConnectionStrategy {
    /**
     * This method *may* return invalid Parameters, RCLS will check if a connection is possible with#
     *  these parameters.
     *
     */
    RiotClientConnectionParameters connect() throws Exception;

    /**
     * This method *should* not throw and *must* clean up all (maybe during {@link #connect()}) acquired resources
     * */
    void disconnect() throws Exception;
}
