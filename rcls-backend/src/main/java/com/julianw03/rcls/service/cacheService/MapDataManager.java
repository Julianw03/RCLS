package com.julianw03.rcls.service.cacheService;

import com.julianw03.rcls.service.riotclient.RiotClientService;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link MapDataManager} is being backed by a thread-safe {@link Map} implementation.
 * This is useful when changes to single keys are being made, and the entire map is not (frequently) changed.
 *
 * @param <K> The key type
 * @param <V> The value type
 *
 * */
public abstract class MapDataManager<K, V> extends DataManager<Map<K, V>>{

    protected final Map<K, V> map = new ConcurrentHashMap<>();

    protected MapDataManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }

    protected final void initialize() {
        cacheService.addMapDataManager(this);
        doInit();
    }

    @Override
    public void setState(Map<K, V> state) {
        this.map.clear();
        this.map.putAll(state);
    }

    @Override
    public Map<K, V> getState() {
        return map;
    }

    @Override
    public void resetInternalState() {
        this.map.clear();
    }
}
