package com.julianw03.rcls.service.base.riotclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.config.mappings.RiotClientServiceConfig;
import com.julianw03.rcls.generated.ApiClient;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;
import com.julianw03.rcls.service.base.riotclient.api.RiotClientError;
import com.julianw03.rcls.service.base.riotclient.connection.RiotClientConnectionStrategy;
import com.julianw03.rcls.service.base.riotclient.ssl.RiotSSLContext;
import feign.Client;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class RiotClientServiceImpl extends RiotClientService {

    private static final Logger log = LoggerFactory.getLogger(RiotClientServiceImpl.class);

    private final ProcessService               processService;
    private final RiotClientServiceConfig      config;
    private final RiotClientConnectionStrategy connectionStrategy;

    private final ScheduledExecutorService                           futureExecutorService;
    private final ExecutorService                                    messageExecutorService;
    private final AtomicReference<ConnectionState>                   connectionStateRef;
    private       RiotClientConnectionParameters                     parameters;
    private       ApiClient                                          apiClient;
    private final Map<Class<? extends ApiClient.Api>, ApiClient.Api> apiClientMap;

    private final SSLContext sslContext;
    private final HttpClient httpClient;

    private final ObjectMapper mapper;

    private final List<RCUMessageListener> listeners;
    private       WebSocket                socket;


    public RiotClientServiceImpl(
            @Autowired ProcessService processService,
            RiotClientConnectionStrategy connectionStrategy,
            RiotClientServiceConfig config
    ) {
        this.processService = processService;
        this.config = config;
        this.connectionStateRef = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.connectionStrategy = connectionStrategy;
        this.apiClientMap = new ConcurrentHashMap<>();
        this.mapper = new ObjectMapper();
        this.futureExecutorService = Executors.newScheduledThreadPool(10);
        this.messageExecutorService = Executors.newFixedThreadPool(10);
        //As we perform significantly more reads than writes this should be okay
        this.listeners = new CopyOnWriteArrayList<>();
        try {
            this.sslContext = RiotSSLContext.create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).sslContext(sslContext).build();
    }

    @Override
    public RiotClientConnectionParameters getConnectionParameters() {
        return this.parameters;
    }

    @Override
    public boolean isConnectionEstablished() {
        return connectionStateRef.get() == ConnectionState.CONNECTED;
    }

    @Override
    public void connect() throws IllegalStateException, UnsupportedOperationException, ExecutionException {
        if (!this.connectionStateRef.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.WAITING_FOR_PROCESS)) {
            log.info("Connect called while currently in state of connecting, returning");
            throw new IllegalStateException("Currently already in the state of connecting, will not start attempt");
        }

        final RiotClientConnectionParameters connectionParameters;
        try {
            connectionParameters = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return connectionStrategy.connect();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, futureExecutorService)
                    .orTimeout(config.getConnectionStrategy().getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException | CancellationException e) {
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new ExecutionException("Failed to connect to process", e);
        }

        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_PROCESS, ConnectionState.WAITING_FOR_REST_READY)) {
            log.error("Service statemachine expected different state");
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new IllegalStateException("Service statemachine expected different state");
        }

        try {
            awaitRestReady(connectionParameters).orTimeout(config.getConnectionInit().getRestConnectWaitForMaxMs(), TimeUnit.MILLISECONDS).join();
        } catch (CancellationException | CompletionException e) {
            log.error("Failed to establish REST connection in given timeout", e);
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new ExecutionException("Failed to establish REST connection in given timeout", e);
        }

        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_REST_READY, ConnectionState.WAITING_FOR_WEBSOCKET_CONNECTION)) {
            log.error("Service statemachine expected different state");
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new IllegalStateException("Service statemachine expected different state");
        }
        this.parameters = connectionParameters;

        try {
            this.socket = awaitWebsocketConnection(connectionParameters).orTimeout(3, TimeUnit.SECONDS).get();
        } catch (CancellationException | CompletionException | InterruptedException | ExecutionException e) {
            log.error("Failed to establish Websocket Connection in given timeout", e);
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to establish Websocket Connection in given timeout");
        }

        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_WEBSOCKET_CONNECTION, ConnectionState.CONNECTED)) {
            log.error("Service statemachine expected different state");
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new IllegalStateException("Service statemachine expected different state");
        }
    }

    @Override
    public void disconnect() throws IllegalStateException, UnsupportedOperationException, ExecutionException {
        try {
            connectionStrategy.disconnect();
        } catch (Exception e) {
            throw new ExecutionException("Failed to disconnect from process", e);
        }
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "Client requested close");
    }

    @Override
    public void addMessageListener(RCUMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(RCUMessageListener listener) {
        listeners.remove(listener);
    }

    public InternalApiResponse request(HttpMethod method, String relativePath, Object body) {
        if (connectionStateRef.get() != ConnectionState.CONNECTED)
            return new InternalApiResponse.InternalException(null);
        JsonNode bodyNode = mapper.valueToTree(body);
        HttpRequest.BodyPublisher bodyPublisher = (this.methodAllowsBodyPublishing(method) && !bodyNode.isNull()) ? HttpRequest.BodyPublishers.ofString(bodyNode.toString()) : HttpRequest.BodyPublishers.noBody();
        HttpRequest request = HttpRequest.newBuilder()
                .method(method.name(), bodyPublisher)
                .uri(URI.create("https://127.0.0.1:" + parameters.getPort() + relativePath))
                .header(HttpHeaders.AUTHORIZATION, parameters.getAuthHeader())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();

        try {
            HttpResponse<JsonNode> response = httpClient.send(
                    request,
                    (resp) -> HttpResponse.BodySubscribers.mapping(
                            HttpResponse.BodySubscribers.ofInputStream(),
                            (is) -> {
                                try (is) {
                                    return mapper.readTree(is);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return NullNode.instance;
                                }
                            }
                    )
            );

            if (!ServletUtils.isSuccessResponseCode(response.statusCode())) {
                //We will assume that the Riot Client will return a default Error here then
                return new InternalApiResponse.ApiError(mapper.treeToValue(response.body(), RiotClientError.class));
            }

            if ((HttpStatus.NO_CONTENT.value() == response.statusCode()) || response.body().isEmpty()) {
                return new InternalApiResponse.NoContent();
            }

            return new InternalApiResponse.Success(response.body());
        } catch (Exception e) {
            return new InternalApiResponse.InternalException(e);
        }
    }

    @Override
    public <T> Optional<HttpResponse<T>> request(HttpMethod method, String relativePath, Object body, Class<T> targetClass) {
        if (connectionStateRef.get() != ConnectionState.CONNECTED) return Optional.empty();
        JsonNode bodyNode = mapper.valueToTree(body);
        HttpRequest.BodyPublisher bodyPublisher = (this.methodAllowsBodyPublishing(method) && !bodyNode.isNull()) ? HttpRequest.BodyPublishers.ofString(bodyNode.toString()) : HttpRequest.BodyPublishers.noBody();
        HttpRequest request = HttpRequest.newBuilder()
                .method(method.name(), bodyPublisher)
                .uri(URI.create("https://127.0.0.1:" + parameters.getPort() + relativePath))
                .header(HttpHeaders.AUTHORIZATION, parameters.getAuthHeader())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();


        try {
            HttpResponse<T> response = httpClient.send(
                    request,
                    (resp) -> HttpResponse.BodySubscribers.mapping(
                            HttpResponse.BodySubscribers.ofInputStream(),
                            (is) -> {
                                try (is) {
                                    return mapper.readValue(is, targetClass);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                    )
            );

            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Exception while requesting proxy access to {} ", request.uri(), e);
        }
        return Optional.empty();
    }

    private boolean methodAllowsBodyPublishing(HttpMethod method) {
        return !method.equals(HttpMethod.GET) && !method.equals(HttpMethod.HEAD);
    }

    @Override
    public Optional<ApiClient> getApiClient() {
        if (connectionStateRef.get() != ConnectionState.CONNECTED) return Optional.empty();
        if (this.apiClient == null) {
            final ApiClient apiClient =
                    new ApiClient()
                            .setBasePath("https://127.0.0.1:" + parameters.getPort())
                            .setFeignBuilder(
                                    new feign.Feign.Builder()
                                            .client(new Client.Default(
                                                    sslContext.getSocketFactory(),
                                                    (hostname, session) -> HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                                            ))
                                            .requestInterceptor((request) -> {
                                                RiotClientConnectionParameters parameters = getConnectionParameters();
                                                if (parameters == null) return;

                                                request.header(HttpHeaders.AUTHORIZATION, parameters.getAuthHeader());
                                            })
                                            .encoder(new feign.jackson.JacksonEncoder(mapper))
                                            .decoder(new feign.jackson.JacksonDecoder(mapper))
                            );
            this.apiClient = apiClient;
            this.apiClient.addAuthorization("basicAuth", new RequestInterceptor() {
                @Override
                public void apply(RequestTemplate template) {
                    template.header(HttpHeaders.AUTHORIZATION, parameters.getAuthHeader());
                }
            });
        }
        return Optional.of(apiClient);
    }

    @Override
    public <T extends ApiClient.Api> Optional<T> getApi(Class<T> apiClass) {
        if (apiClass == null) return Optional.empty();
        return getApiClient().map(client ->
                apiClass.cast(apiClientMap.computeIfAbsent(apiClass, client::buildClient))
        );
    }

    @Override
    public void startup() {
        log.info("Startup called");
        long start = System.currentTimeMillis();

        long end = System.currentTimeMillis();
        log.info("Startup succeeded after {}ms", end - start);
    }

    @Override
    public void shutdown() {
        log.info("Shutdown called");
        long start = System.currentTimeMillis();

        this.parameters = null;
        this.futureExecutorService.shutdown();
        this.listeners.clear();
        try {
            this.futureExecutorService.awaitTermination(800, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            this.futureExecutorService.shutdownNow();
        }

        long end = System.currentTimeMillis();
        log.info("Shutdown succeeded after {}ms", (end - start));
    }

    public CompletableFuture<WebSocket> awaitWebsocketConnection(RiotClientConnectionParameters parameters) {
        CompletableFuture<WebSocket> future = new CompletableFuture<>();
        futureExecutorService.submit(() -> {
            httpClient
                    .newWebSocketBuilder()
                    .header(HttpHeaders.AUTHORIZATION, parameters.getAuthHeader())
                    .buildAsync(URI.create("wss://127.0.0.1:" + parameters.getPort()), new WebSocket.Listener() {
                        private final StringBuffer stringBuffer = new StringBuffer();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            future.complete(webSocket);
                            futureExecutorService.submit(() -> {
                                ArrayNode node = mapper.createArrayNode();
                                node.add(5);
                                node.add("OnJsonApiEvent");

                                webSocket.sendText(node.toString(), true);
                            });
                            futureExecutorService.submit(() -> {
                                for (RCUMessageListener listener : listeners) {
                                    listener.onConnect();
                                }
                            });
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            stringBuffer.append(data);

                            if (last) {
                                final String message = stringBuffer.toString();
                                stringBuffer.setLength(0);

                                messageExecutorService.submit(() -> {
                                    Thread.currentThread().setName("Message-Dispatch");
                                    if (message.isEmpty()) return;
                                    final RCUWebsocketMessage parsedMessage;
                                    try {
                                        ArrayNode messageNode = (ArrayNode) mapper.readTree(message);
                                        if (messageNode.size() != 3) return;
                                        parsedMessage = mapper.convertValue(messageNode.get(2), RCUWebsocketMessage.class);
                                    } catch (Exception e) {
                                        log.warn("The message \"{}\" could not be parsed", message, e);
                                        return;
                                    }
                                    log.debug("{} - {}: {}", parsedMessage.getType(), parsedMessage.getUri(), parsedMessage.getData());
                                    for (RCUMessageListener listener : listeners) {
                                        listener.onMessage(parsedMessage);
                                    }
                                });
                            }

                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            futureExecutorService.submit(() -> {
                                handleWebsocketClosed();
                            });
                            futureExecutorService.submit(() -> {
                                for (RCUMessageListener listener : listeners) {
                                    listener.onDisconnect();
                                }
                            });
                            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                        }

                        ;
                    });
        });
        return future;
    }

    public CompletableFuture<Void> awaitRestReady(RiotClientConnectionParameters parametersToTest) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        futureExecutorService.schedule(new Runnable() {
            private AtomicInteger attempts = new AtomicInteger(0);

            @Override
            public void run() {
                if (attempts.addAndGet(1) > config.getConnectionInit().getRestConnectAttempts()) {
                    future.completeExceptionally(new Exception("Failed to establish within given attempts"));
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + parametersToTest.getPort() + "/riotclientapp/v1/command-line-args"))
                        .GET()
                        .header(HttpHeaders.AUTHORIZATION, parametersToTest.getAuthHeader())
                        .build();

                if (testRestConnection(request, parameters)) {
                    future.complete(null);
                    return;
                }

                futureExecutorService.schedule(this, config.getConnectionInit().getRestConnectDelayMs(), TimeUnit.MILLISECONDS);
            }
        }, config.getConnectionInit().getRestConnectDelayMs(), TimeUnit.MILLISECONDS);
        return future;
    }

    private boolean testRestConnection(HttpRequest request, RiotClientConnectionParameters parameters) {
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value()) return false;
            Optional<JsonNode> parsedResponse = Utils.parseJson(mapper, response.body());
            if (parsedResponse.isEmpty()) return false;
            JsonNode responseNode = parsedResponse.get();
            return responseNode.isArray();
        } catch (Exception e) {
            log.error("Error while trying to test connection");
        }
        return false;
    }

    private void handleWebsocketClosed() {
        if (!connectionStateRef.compareAndSet(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED)) {
            log.warn("Illegal state while websocket close notified");
        }
        this.socket = null;
        this.apiClient = null;
        this.apiClientMap.clear();
        this.parameters = null;
    }

    private RiotClientConnectionParameters generateParameters() {
        Integer port = Utils.getFreePort();
        SecureRandom random = new SecureRandom();
        byte[] randomToGenerate = new byte[18];
        random.nextBytes(randomToGenerate);
        return new RiotClientConnectionParameters(Base64.getEncoder().encodeToString(randomToGenerate), port);
    }
}
