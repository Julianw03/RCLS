package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.service.base.publisher.formats.StateUpdateFormat;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;

public abstract class PublishingObjectDataManager<T> extends ObjectDataManager<T>{
    protected PublishingObjectDataManager(RiotClientService riotClientService, StateService cacheService) {
        super(riotClientService, cacheService);
    }

    @Override
    protected void onStateUpdated(T prevState, T newState) {
        super.onStateUpdated(prevState, newState);
        cacheService.onManagerUpdate(new StateUpdateFormat<>(getUri(), newState));
    }

    protected String getUri() {
        return this.getClass().getSimpleName();
    }
}
