package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.BaseService;
import com.julianw03.rcls.service.base.publisher.PublisherMessage;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;


@Slf4j
@Component
public class CacheService extends BaseService implements RCUMessageListener {

    private final Map<Class<? extends MapDataManager>, MapDataManager<?, ?>>    mapDataManagerMap;
    private final Map<Class<? extends ObjectDataManager>, ObjectDataManager<?>> objectDataManagerMap;
    private final ExecutorService                                               executorService = Executors.newFixedThreadPool(10);

    public <V extends MapDataManager<?, ?>> V getMapDataManager(Class<V> clazz) {
        if (clazz == null) return null;
        return (V) mapDataManagerMap.get(clazz);
    }

    public void addMapDataManager(MapDataManager<?, ?> mapDataManager) {
        if (mapDataManager == null) return;
        if (mapDataManagerMap.containsKey(mapDataManager.getClass())) {
            log.warn("MapDataManager {} already exists, not adding", mapDataManager.getClass());
            return;
        }
        mapDataManagerMap.put(mapDataManager.getClass(), mapDataManager);
    }

    public <V extends ObjectDataManager<?>> V getObjectDataManger(Class<V> clazz) {
        if (clazz == null) return null;
        return (V) objectDataManagerMap.get(clazz);
    }

    public void addObjectDataManager(ObjectDataManager<?> objectDataManager) {
        if (objectDataManager == null) return;
        if (objectDataManagerMap.containsKey(objectDataManager.getClass())) {
            log.warn("ObjectDataManager {} already exists, not adding", objectDataManager.getClass());
            return;
        }
        objectDataManagerMap.put(objectDataManager.getClass(), objectDataManager);
    }

    public CacheService() {
        mapDataManagerMap = new ConcurrentHashMap<>();
        objectDataManagerMap = new ConcurrentHashMap<>();
    }

    @Override
    public void startup() {
        log.info("Starting up cache service");
    }

    @Override
    public void shutdown() {
        log.info("Shutdown called");
        long now = System.currentTimeMillis();

        mapDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::reset));
        objectDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::reset));

        mapDataManagerMap.clear();
        objectDataManagerMap.clear();

        long elapsed = System.currentTimeMillis() - now;
        log.info("Shutdown succeeded after {}ms", elapsed);
    }

    @Override
    public void onConnect() {
        RCUMessageListener.super.onConnect();
        CompletableFuture<?>[] mapSetupFutures = mapDataManagerMap.values().stream()
                .map(MapDataManager::setupInternalState)
                .map((future) -> future.exceptionally((ex) -> {
                    log.warn("Failed to setup map data manager", ex);
                    return null;
                }))
                .toArray(CompletableFuture[]::new);
        CompletableFuture<?>[] objectSetupFutures = objectDataManagerMap.values().stream()
                .map(ObjectDataManager::setupInternalState)
                .map((future) -> future.exceptionally((ex) -> {
                    log.warn("Failed to setup object data manager", ex);
                    return null;
                }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture<Void> mapSetupFuture = CompletableFuture.allOf(mapSetupFutures);
        CompletableFuture<Void> objectSetupFuture = CompletableFuture.allOf(objectSetupFutures);

        CompletableFuture<Void> allSetupFuture = CompletableFuture.allOf(
                mapSetupFuture,
                objectSetupFuture
        );

        try {
            long startTime = System.currentTimeMillis();
            allSetupFuture.join();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Setup succeeded after {}ms", elapsedTime);
        } catch (CancellationException | CompletionException e) {
            log.error("Setup failed", e);
        }
        ;
    }

    @Override
    public void onMessage(RCUWebsocketMessage message) {
        if (message == null) return;
        if (message.getType() == null) return;
        if (message.getUri() == null) return;
        if (message.getData() == null) return;

        dispatchMessage(message);
    }

    private void dispatchMessage(RCUWebsocketMessage message) {
        executorService.submit(() -> {
            for (MapDataManager<?, ?> dataManager : mapDataManagerMap.values()) {
                Utils.wrapSecure(() -> dataManager.onRCUMessage(message));
            }
            for (ObjectDataManager<?> dataManager : objectDataManagerMap.values()) {
                Utils.wrapSecure(() -> dataManager.onRCUMessage(message));
            }
        });
    }

    public void onManagerUpdate(PublisherFormat format) {

    }

    @Override
    public void onDisconnect() {
        RCUMessageListener.super.onDisconnect();
        mapDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::resetInternalState));
        objectDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::resetInternalState));
    }
}
