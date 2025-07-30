package com.julianw03.rcls.service.base.cacheService.rcu;

import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.MapDataManager;
import com.julianw03.rcls.service.base.cacheService.ObjectDataManager;
import com.julianw03.rcls.service.base.cacheService.StateService;
import com.julianw03.rcls.service.base.publisher.PublisherService;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
public class RCUStateService extends StateService implements RCUMessageListener {
    public RCUStateService(PublisherService publisherService) {
        super(publisherService);
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

    @Override
    public void onManagerUpdate(PublisherFormat format) {
        executorService.submit(() -> {
            publisherService.dispatchChange(
                    PublisherService.Source.RCU_STATE_SERVICE,
                    format
            );
        });
    }

    @Override
    public void onDisconnect() {
        RCUMessageListener.super.onDisconnect();
        mapDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::resetInternalState));
        objectDataManagerMap.values().forEach((dataManager) -> Utils.wrapSecure(dataManager::resetInternalState));
    }
}
