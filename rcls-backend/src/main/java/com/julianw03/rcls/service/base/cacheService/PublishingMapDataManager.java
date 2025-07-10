package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.service.base.publisher.formats.MapKeyFormat;
import com.julianw03.rcls.service.base.publisher.formats.StateUpdateFormat;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;

import java.util.Map;

public abstract class PublishingMapDataManager<K, V> extends MapDataManager<K, V> {
    protected PublishingMapDataManager(RiotClientService riotClientService, StateService cacheService) {
        super(riotClientService, cacheService);
    }

    @Override
    protected void onKeyUpdated(K key, V previousValue, V newValue) {
        cacheService.onManagerUpdate(new MapKeyFormat<>(getUri(), key, newValue));
    }

    @Override
    protected void onStateUpdated(Map<K, V> previousState, Map<K, V> newState) {
        cacheService.onManagerUpdate(new StateUpdateFormat<>(getUri(), newState));
    }

    protected String getUri() {
        return this.getClass().getSimpleName();
    }
}
