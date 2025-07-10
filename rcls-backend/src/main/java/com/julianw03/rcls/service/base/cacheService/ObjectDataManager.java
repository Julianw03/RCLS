package com.julianw03.rcls.service.base.cacheService;

import com.julianw03.rcls.service.base.riotclient.RiotClientService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link ObjectDataManager}
 *
 * */
public abstract class ObjectDataManager<T> extends DataManager<T> {

    private final AtomicReference<T> objectRef = new AtomicReference<>(null);

    protected ObjectDataManager(RiotClientService riotClientService, StateService cacheService) {
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
        final T previousState = objectRef.getAndSet(state);
        if (Objects.equals(previousState, state)) {
            return;
        }
        onStateUpdated(previousState, state);
    }

    @Override
    public void resetInternalState() {
        this.objectRef.set(null);
    }
}
