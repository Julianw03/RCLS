package com.julianw03.rcls.service.riotclient;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.BaseService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import org.springframework.http.HttpMethod;

import java.net.http.HttpResponse;
import java.util.Optional;

public abstract class RiotClientService extends BaseService {
    /**
     * @return {@link RiotClientConnectionParameters} if they are currently deemed valid by the service otherwise {@code null}
     */
    public abstract RiotClientConnectionParameters getConnectionParameters();

    public abstract boolean isConnectionEstablished();

    public abstract void connect() throws APIException;

    public abstract void addMessageListener(RCUMessageListener listener);

    public abstract void removeMessageListener(RCUMessageListener listener);

    public abstract <T> Optional<HttpResponse<T>> request(HttpMethod method, String relativePath, Object body, Class<T> responseClass);


    public abstract InternalApiResponse request(HttpMethod method, String relativePath, Object body);

}
