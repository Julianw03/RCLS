package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.service.base.riotclient.RiotClientService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link MapDataManager} is being backed by a thread-safe {@link Map} implementation.
 * This is useful when changes to single keys are being made, and the entire map is not (frequently) changed.
 *
 * @param <K> The key type
 * @param <V> The value type
 *
 * */
public abstract class MapDataManager<K, V> extends DataManager<Map<K, V>> implements Map<K, V>{

    private final Map<K, V> map = new ConcurrentHashMap<>();

    protected MapDataManager(RiotClientService riotClientService, StateService cacheService) {
        super(riotClientService, cacheService);
    }

    protected final void initialize() {
        cacheService.addMapDataManager(this);
        doInit();
    }

    @Override
    public void setState(Map<K, V> state) {
        if (Objects.equals(state, map)) {
            return;
        }

        final Map<K, V> prevMap = Collections.unmodifiableMap(map);

        this.map.clear();
        this.map.putAll(state);

        this.onStateUpdated(prevMap, map);
    }

    @Override
    public Map<K, V> getState() {
        return map;
    }

    /**
     * THIS IS CALLED INSIDE SOME ATOMIC OPERATION, SO DO NOT CALL ANYTHING THAT MIGHT BLOCK OR CAUSE A DEADLOCK!
     * */
    protected void onKeyUpdated(K key, V previousValue, V newValue) {
        // This method can be overridden to handle key updates
    }

    public V put(K key, V value) {
        return map.compute(key, (k, previousValue) -> {
            if (Objects.equals(previousValue, value)) {
                return value;
            }
            onKeyUpdated(k, previousValue, value);
            return value;
        });
    }

    public V remove(Object key) {
        return map.compute((K) key, (k, previousValue) -> {
            if (previousValue != null) {
                onKeyUpdated((K) k, previousValue, null);
            }

            return null;
        });
    }

    public V get(Object key) {
        return this.map.get(key);
    }

    @Override
    public void resetInternalState() {
        this.map.clear();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public void clear() {
        setState(Collections.emptyMap());
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null || m.isEmpty()) {
            return;
        }

        Map<K, V> prevMap = Collections.unmodifiableMap(m);
        this.map.putAll(m);

        onStateUpdated(prevMap, this.map);
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapDataManager)) return false;
        MapDataManager<?, ?> that = (MapDataManager<?, ?>) o;
        return this.map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }
}
