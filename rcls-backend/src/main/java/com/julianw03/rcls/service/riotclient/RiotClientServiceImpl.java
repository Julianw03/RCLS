package com.julianw03.rcls.service.riotclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.julianw03.rcls.Util.ServletUtils;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.*;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;
import com.julianw03.rcls.service.riotclient.api.RiotClientError;
import com.julianw03.rcls.service.riotclient.ssl.RiotSSLContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


public class RiotClientServiceImpl extends RiotClientService {

    private static final Logger log = LoggerFactory.getLogger(RiotClientServiceImpl.class);

    private final ProcessService                   processService;
    private final ExecutorService                  futureExecutorService;
    private final ExecutorService                  messageExecutorService;
    private final AtomicReference<ConnectionState> connectionStateRef;
    private final SSLContext                       sslContext;
    private final HttpClient                       httpClient;
    private final ObjectMapper                     mapper;
    private final List<RCUMessageListener>         listeners;
    private       RiotClientConnectionParameters   parameters;
    private       WebSocket                        socket;



    public RiotClientServiceImpl(ProcessService processService) {
        this.processService = processService;
        this.connectionStateRef = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.mapper = new ObjectMapper();
        this.futureExecutorService = Executors.newFixedThreadPool(10);
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
    public void connect() throws APIException {
        if (!this.connectionStateRef.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.WAITING_FOR_PROCESS)) {
            log.warn("Connect called while currently in state of connecting, returning");
            throw new APIException("Invalid state", "Currently already in the state of connecting, will not start attempt", HttpStatus.CONFLICT);
        }

        RiotClientConnectionParameters parameters = generateParameters();
        log.info("Generated parameters: {}", parameters);
        CompletableFuture<?>[] killGameFutures = new CompletableFuture<?>[SupportedGame.values().length];

        int i = 0;
        for (SupportedGame game : SupportedGame.values()) {
            killGameFutures[i] = processService.killGameProcess(game)
                    .exceptionally(ex -> {
                        if (ex instanceof CompletionException) {
                            if (ex.getCause() instanceof UnsupportedOperationException) {
                                log.warn("Game {} is not supported on this OS", game);
                            }
                            return null;
                        }
                        throw new RuntimeException("Failed to kill game process", ex);
                    });
            i++;
        }

        try {
            CompletableFuture.allOf(killGameFutures).join();
        } catch (Exception e) {
            log.error("Failed to kill all running games", e);
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to kill all games", "This application needs to kill all games (as keeping them running while killing the RCS will lead to errors)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.info("Successfully killed all games");

        try {
            processService.killRiotClientProcess().join();
        } catch (Exception e) {
            log.error("Failed to kill current Riot Client instance", e);
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to kill previous Riot Client Instance");
        }

        try {
            processService.killRiotClientServices().join();
        } catch (Exception e) {
            log.error("Failed to kill current Riot Client services instance", e);
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to kill previous Riot Client Service Instance", "Maybe you have started Riot Client Services with a higher permission level?", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.info("Killed previous Riot Client Services instance");

        try {
            processService.startRiotClientServices(parameters).join();
        } catch (Exception e) {
            //Error occurred, reset back
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to start new Riot Client Service Instance");
        }
        log.info("Created new Riot Client Service Instance with current Application as Manager");
        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_PROCESS, ConnectionState.WAITING_FOR_REST_READY)) {
            log.error("Service statemachine expected different state");
            return;
        }

        try {
            awaitRestReady(parameters).orTimeout(3, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to establish REST connection in given timeout");
        }

        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_REST_READY, ConnectionState.WAITING_FOR_WEBSOCKET_CONNECTION)) {
            log.error("Service statemachine expected");
        }
        this.parameters = parameters;

        try {
            this.socket = awaitWebsocketConnection(parameters).orTimeout(3, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            this.connectionStateRef.set(ConnectionState.DISCONNECTED);
            throw new APIException("Failed to establish Websocket Connection in given timeout");
        }

        if (!this.connectionStateRef.compareAndSet(ConnectionState.WAITING_FOR_WEBSOCKET_CONNECTION, ConnectionState.CONNECTED)) {
            log.error("Service statemachine expected different state");
        }
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
        if (connectionStateRef.get() != ConnectionState.CONNECTED) return new InternalApiResponse.InternalException(null);
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
                                        log.error("The message \"{}\" could not be parsed", message, e);
                                        return;
                                    }
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
        futureExecutorService.submit(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://127.0.0.1:" + parametersToTest.getPort() + "/riotclientapp/v1/command-line-args"))
                    .GET()
                    .header(HttpHeaders.AUTHORIZATION, parametersToTest.getAuthHeader())
                    .build();
            for (int i = 0; i < 10; i++) {
                if (testRestConnection(request, parameters)) {
                    future.complete(null);
                    return;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    future.completeExceptionally(ex);
                    return;
                }
            }
            future.completeExceptionally(new Exception(""));

        });
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
