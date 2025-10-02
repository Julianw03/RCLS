package com.julianw03.rcls.model.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.RCUConnectionEvent;
import com.julianw03.rcls.eventBus.model.events.RCUMessageEvent;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.BaseService;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

/**
 * @param <T> The actual
 */

public abstract class DataManager<T> extends BaseService {
    protected final RiotClientService riotClientService;
    protected final MultiChannelBus   eventBus;
    protected       Disposable        dataSubscription;
    private         Disposable        connectionStateSubscription;
    protected final ObjectMapper      objectMapper     = new ObjectMapper();
    protected final AtomicBoolean     initialFetchDone = new AtomicBoolean(false);
    protected final Logger            log              = LoggerFactory.getLogger(this.getClass());

    public CompletableFuture<Void> setupInternalState() {
        return doFetchInitialData().thenAccept(this::setState)
                                   .thenRun(() -> {
                                       if (!initialFetchDone.compareAndSet(
                                               false,
                                               true
                                       )) {
                                           log.warn(
                                                   "Initial data fetch was already completed, but setupInternalState was called again. This might indicate a reconnect event."
                                           );
                                       } else {
                                           log.info("Initial data fetch completed successfully.");
                                       }
                                   });
    }

    protected abstract CompletableFuture<T> doFetchInitialData();

    protected abstract Matcher getUriMatcher(String uri);

    @Autowired
    protected DataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        this.riotClientService = riotClientService;
        this.eventBus = eventBus;
    }

    @Override
    public void startup() {
        super.startup();
        log.trace(
                "Registering RCU message listener for DataManager: {}",
                this.getClass()
                    .getSimpleName()
        );
        this.dataSubscription = eventBus.getFlux(
                                                Channel.RCU_PROXY,
                                                RCUMessageEvent.class
                                        )
                                        .concatMap(event -> Mono.fromRunnable(() -> onRCUMessage(event))
                                                                .onErrorResume(err -> {
                                                                    log.error(
                                                                            "Failed to process RCU message in DataManager",
                                                                            err
                                                                    );
                                                                    return Mono.empty(); // continue with next
                                                                }))
                                        .subscribe();
        this.connectionStateSubscription = eventBus.getFlux(
                                                           Channel.RCU_CONNECTION_STATE,
                                                           RCUConnectionEvent.class
                                                   )
                                                   .doOnNext((event) -> {
                                                       if (event.getPayload()) {
                                                           setupInternalState();
                                                       } else {
                                                           reset();
                                                       }
                                                   })
                                                   .onErrorContinue((err, obj) -> log.error(
                                                           "Failed to process RCU connection state event",
                                                           err
                                                   ))
                                                   .subscribe();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        log.trace(
                "Unregistering RCU Message listeners for DataManager: {}",
                this.getClass()
                    .getSimpleName()
        );
        Optional.of(dataSubscription)
                .ifPresent(Disposable::dispose);
        Optional.of(connectionStateSubscription)
                .ifPresent(Disposable::dispose);
    }

    protected abstract void handleUpdate(
            RCUWebsocketMessage.MessageType type,
            JsonNode data,
            Matcher uriMatcher
    );

    protected abstract void setState(T state);

    /**
     * THIS MIGHT BE CALLED INSIDE SOME ATOMIC OPERATION, SO DO NOT CALL ANYTHING THAT MIGHT BLOCK OR CAUSE A DEADLOCK!
     */
    protected void onStateUpdated(
            T previousState,
            T newState
    ) {
        // Override this method to handle state updates if needed
    }

    public void reset() {
        initialFetchDone.set(false);
        resetInternalState();
    }

    public abstract void resetInternalState();

    protected abstract T getState();

    public final void onRCUMessage(RCUMessageEvent event) {
        RCUWebsocketMessage item = event.getPayload();
        Matcher uriMatcher = getUriMatcher(item.getUri());
        if (!uriMatcher.matches()) return;
        handleUpdate(
                item.getType(),
                item.getData(),
                uriMatcher
        );
    }

    protected <F> Optional<F> parseJson(
            JsonNode node,
            Class<F> tClass
    ) {
        return Utils.parseJson(
                objectMapper,
                node,
                tClass
        );
    }
}
