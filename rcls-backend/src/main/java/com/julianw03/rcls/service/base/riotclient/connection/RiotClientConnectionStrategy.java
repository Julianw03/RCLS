package com.julianw03.rcls.service.base.riotclient.connection;

import com.julianw03.rcls.model.RiotClientConnectionParameters;

public interface RiotClientConnectionStrategy {
    RiotClientConnectionParameters connect() throws Exception;
    void disconnect() throws Exception;
}
