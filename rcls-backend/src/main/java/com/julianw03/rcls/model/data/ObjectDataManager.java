package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.jetbrains.annotations.Contract;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link ObjectDataManager}
 */
public abstract class ObjectDataManager<T, V> extends DataManager<T> implements ObjectDataManagerFacade<V> {

    private final AtomicReference<T> objectRef = new AtomicReference<>(null);

    protected ObjectDataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
    }

    public V getView() {
        return mapView(this.getState());
    }

    @Contract(pure = true)
    protected abstract V mapView(T state);

    @Override
    protected T getState() {
        return objectRef.get();
    }

    @Override
    public void setState(T state) {
        final T previousState = objectRef.getAndSet(state);
        if (Objects.equals(
                previousState,
                state
        )) {
            log.debug("State is equal to the current state, skipping update");
            return;
        }
        onStateUpdated(
                previousState,
                state
        );
    }

    @Override
    public void resetInternalState() {
        log.debug("Resetting internal state");
        this.setState(null);
    }
}
