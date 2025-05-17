package com.julianw03.rcls.service.base.cacheService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

/**
 * @param <T> The actual
 */

@Slf4j
public abstract class DataManager<T> {
    protected final RiotClientService riotClientService;
    protected final CacheService      cacheService;
    protected final ObjectMapper      objectMapper = new ObjectMapper();

    private final AtomicReference<Flow.Subscription> cacheServiceSubscription = new AtomicReference<>(null);

    public CompletableFuture<Void> setupInternalState() {
        return doFetchInitialData().thenAccept(this::setState);
    }

    @PostConstruct
    protected abstract void initialize();

    protected void doInit() {}


    protected abstract CompletableFuture<T> doFetchInitialData();

    protected abstract Matcher getUriMatcher(String uri);

    @Autowired
    protected DataManager(
            RiotClientService riotClientService,
            CacheService cacheService
    ) {
        this.riotClientService = riotClientService;
        this.cacheService = cacheService;
    }

    protected abstract void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher);

    protected abstract void setState(T state);

    public void reset() {
        Optional.ofNullable(cacheServiceSubscription.getAndSet(null)).ifPresent(Flow.Subscription::cancel);
        resetInternalState();
    }

    public abstract void resetInternalState();

    public abstract T getState();

    public final void onRCUMessage(RCUWebsocketMessage item) {
        Matcher uriMatcher = getUriMatcher(item.getUri());
        if (!uriMatcher.matches()) return;
        handleUpdate(item.getType(), item.getData(), uriMatcher);
    }

    protected <F> Optional<F> parseJson(JsonNode node, Class<F> tClass) {
        return Utils.parseJson(objectMapper, node, tClass);
    }
}
