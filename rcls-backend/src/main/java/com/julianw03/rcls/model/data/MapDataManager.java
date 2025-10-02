package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.service.riotclient.RiotClientService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The {@link MapDataManager} is being backed by a thread-safe {@link Map} implementation.
 * This is useful when changes to single keys are being made, and the entire map is not (frequently) changed.
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public abstract class MapDataManager<K, V, E> extends DataManager<Map<K, V>> implements Map<K, V>, MapDataManagerFacade<K, E> {

    private final Map<K, V> map = new ConcurrentHashMap<>();

    protected MapDataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
    }

    protected E getViewForValue(V value) {
        return mapValueView(value);
    }

    public E getViewForKey(Object key) {
        return mapValueView(get(key));
    }

    protected Map<K, E> mapView(Map<K, V> map) {
        return this.map.entrySet()
                       .stream()
                       .map(entry -> {
                           K key = entry.getKey();
                           V value = entry.getValue();
                           return Map.entry(
                                   key,
                                   mapValueView(value)
                           );
                       })
                       .collect(Collectors.toMap(
                               Map.Entry::getKey,
                               Map.Entry::getValue
                       ));
    }

    @Override
    public Map<K, E> getView() {
        return mapView(this.map);
    }

    protected abstract E mapValueView(V value);

    @Override
    public void setState(Map<K, V> state) {
        if (Objects.equals(
                state,
                map
        )) {
            log.debug("New state is equal to the current state, skipping update");
            return;
        }

        log.debug("Updating internal map state");
        final Map<K, V> prevMap = Collections.unmodifiableMap(map);

        this.map.clear();
        this.map.putAll(state);

        this.onStateUpdated(
                prevMap,
                map
        );
    }

    @Override
    public Map<K, V> getState() {
        return map;
    }

    /**
     * THIS IS CALLED INSIDE SOME ATOMIC OPERATION, SO DO NOT CALL ANYTHING THAT MIGHT BLOCK OR CAUSE A DEADLOCK!
     */
    protected void onKeyUpdated(
            K key,
            V previousValue,
            V newValue
    ) {
        // This method can be overridden to handle key updates
    }

    public V put(
            K key,
            V value
    ) {
        return map.compute(
                key,
                (k, previousValue) -> {
                    if (Objects.equals(
                            previousValue,
                            value
                    )) {
                        return value;
                    }
                    onKeyUpdated(
                            k,
                            previousValue,
                            value
                    );
                    return value;
                }
        );
    }

    public V remove(Object key) {
        return map.compute(
                (K) key,
                (k, previousValue) -> {
                    if (previousValue != null) {
                        onKeyUpdated(
                                (K) k,
                                previousValue,
                                null
                        );
                    }

                    return null;
                }
        );
    }

    public V get(Object key) {
        return this.map.get(key);
    }

    @Override
    public void resetInternalState() {
        log.debug("Resetting internal map state");
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

        onStateUpdated(
                prevMap,
                this.map
        );
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
        MapDataManager<?, ?, ?> that = (MapDataManager<?, ?, ?>) o;
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
