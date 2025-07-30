package com.julianw03.rcls.service.base.riotclient;

import com.julianw03.rcls.generated.ApiClient;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.BaseService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
import org.springframework.http.HttpMethod;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class RiotClientService extends BaseService {

    public enum SimpleConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    /**
     * @return {@link RiotClientConnectionParameters} if they are currently deemed valid by the service otherwise {@code null}
     */
    public abstract RiotClientConnectionParameters getConnectionParameters();

    public abstract boolean isConnectionEstablished();

    public abstract void connect() throws IllegalStateException, UnsupportedOperationException, ExecutionException;

    public abstract void disconnect() throws IllegalStateException, UnsupportedOperationException, ExecutionException;

    public abstract void addMessageListener(RCUMessageListener listener);

    public abstract void removeMessageListener(RCUMessageListener listener);

    public abstract Optional<ApiClient> getApiClient();

    public abstract <T extends ApiClient.Api> Optional<T> getApi(Class<T> apiClass);

    public abstract <T> Optional<HttpResponse<T>> request(HttpMethod method, String relativePath, Object body, Class<T> responseClass);

    public abstract InternalApiResponse request(HttpMethod method, String relativePath, Object body);
}
