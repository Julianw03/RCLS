package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.model.RCUMessageListener;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.BaseService;
import com.julianw03.rcls.service.base.publisher.PublisherService;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;


@Slf4j
public abstract class StateService extends BaseService {

    protected final Map<Class<? extends MapDataManager>, MapDataManager<?, ?>>    mapDataManagerMap;
    protected final Map<Class<? extends ObjectDataManager>, ObjectDataManager<?>> objectDataManagerMap;
    protected final ExecutorService                                               executorService = Executors.newFixedThreadPool(10);
    protected final PublisherService publisherService;

    public StateService(PublisherService publisherService) {
        mapDataManagerMap = new ConcurrentHashMap<>();
        objectDataManagerMap = new ConcurrentHashMap<>();
        this.publisherService = publisherService;
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

    public abstract void onManagerUpdate(PublisherFormat format);
}
