package com.julianw03.rcls.service.cacheService;

import com.julianw03.rcls.service.riotclient.RiotClientService;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link ObjectDataManager}
 *
 * */
public abstract class ObjectDataManager<T> extends DataManager<T> {

    protected AtomicReference<T> objectRef = new AtomicReference<>(null);

    protected ObjectDataManager(RiotClientService riotClientService, CacheService cacheService) {
        super(riotClientService, cacheService);
    }

    protected final void initialize() {
        cacheService.addObjectDataManager(this);
        doInit();
    }

    @Override
    public T getState() {
        return objectRef.get();
    }

    @Override
    public void setState(T state) {
        this.objectRef.set(state);
    }

    @Override
    public void resetInternalState() {
        this.objectRef.set(null);
    }
}
