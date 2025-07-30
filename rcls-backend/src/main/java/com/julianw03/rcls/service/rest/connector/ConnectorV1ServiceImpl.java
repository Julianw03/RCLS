package com.julianw03.rcls.service.rest.connector;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class ConnectorV1RestService {
    private final RiotClientService riotClientService;

    public ConnectorV1RestService(
            @Autowired RiotClientService riotClientService
    ) {
        this.riotClientService = riotClientService;
    }

    public RiotClientConnectionParameters connectToRiotClient() throws APIException {
        try {
            riotClientService.connect();
        } catch (IllegalStateException e) {
            throw new APIException("Riot Client is already connected", HttpStatus.CONFLICT, e);
        } catch (ExecutionException e) {
            throw new APIException("Failed to connect to Riot Client", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            throw new APIException("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        return riotClientService.getConnectionParameters();
    }

    public RiotClientConnectionParameters getConnectionParameters() throws APIException {
        RiotClientConnectionParameters params = riotClientService.getConnectionParameters();
        if (params == null) {
            throw new APIException("Failed to get Process Parameters", HttpStatus.NOT_FOUND, "Maybe the application is not connected ?");
        }

        return params;
    }
}
