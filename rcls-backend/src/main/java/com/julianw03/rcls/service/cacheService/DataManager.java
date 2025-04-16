package com.julianw03.rcls.service.cacheService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

/**
 * @param <T> The actual
 */

@Slf4j
public abstract class DataManager<T> implements Flow.Subscriber<RCUWebsocketMessage> {
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

    /*
     * Following methods are marked as final to prevent overriding.
     * */

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        if (!this.cacheServiceSubscription.compareAndSet(null, subscription)) {
            //Previous subscription present. Will not update reference and cancel subscription
            subscription.cancel();
            return;
        }
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public final void onNext(RCUWebsocketMessage item) {
        Matcher uriMatcher = getUriMatcher(item.getUri());
        if (!uriMatcher.matches()) return;
        handleUpdate(item.getType(), item.getData(), uriMatcher);
    }

    @Override
    public final void onError(Throwable throwable) {
        log.error("Received error ", throwable);
    }

    @Override
    public final void onComplete() {
        log.warn("On Complete called");
    }

    protected <F> Optional<F> parseJson(JsonNode node, Class<F> tClass) {
        return Utils.parseJson(objectMapper, node, tClass);
    }
}
