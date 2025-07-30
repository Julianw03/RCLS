package com.julianw03.rcls.service.rest.connector;

import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class ConnectorV1ServiceImpl implements ConnectorV1Service {
    private final RiotClientService riotClientService;

    public ConnectorV1ServiceImpl(
            @Autowired RiotClientService riotClientService
    ) {
        this.riotClientService = riotClientService;
    }

    public RiotClientConnectionParametersDTO connect() throws ExecutionException, IllegalStateException {
        riotClientService.connect();
        return this.getConnectionParameters();
    }

    public RiotClientConnectionParametersDTO getConnectionParameters() throws IllegalStateException {
        if (!riotClientService.isConnectionEstablished()) throw new IllegalStateException("Connection is not established.");
        RiotClientConnectionParameters params = riotClientService.getConnectionParameters();
        if (params == null) {
            throw new IllegalStateException("Connection parameters are not available.");
        }

        return fromInternalType(params);
    }

    @Override
    public void disconnect() throws IllegalStateException, ExecutionException {
        riotClientService.disconnect();
    }

    private RiotClientConnectionParametersDTO fromInternalType(RiotClientConnectionParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        return RiotClientConnectionParametersDTO.builder()
                .port(params.getPort())
                .authSecret(params.getAuthSecret())
                .authHeader(params.getAuthHeader())
                .build();
    }
}
