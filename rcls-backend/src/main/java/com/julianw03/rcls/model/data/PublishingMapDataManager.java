package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.*;
import com.julianw03.rcls.eventBus.model.events.KeyViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.KeyViewUpdatedPayload;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedPayload;
import com.julianw03.rcls.service.riotclient.RiotClientService;

import java.util.Map;

public abstract class PublishingMapDataManager<K, V, E> extends MapDataManager<K, V, E> {

    protected PublishingMapDataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
    }

    @Override
    protected void onKeyUpdated(
            K key,
            V previousValue,
            V newValue
    ) {
        eventBus.publish(
                Channel.DATA_MANAGER,
                new KeyViewUpdatedEvent<>(
                        getUri(),
                        new KeyViewUpdatedPayload<>(
                                key,
                                mapValueView(newValue)
                        )
                )
        );
    }

    @Override
    protected void onStateUpdated(
            Map<K, V> previousState,
            Map<K, V> newState
    ) {
        eventBus.publish(
                Channel.DATA_MANAGER,
                new ViewUpdatedEvent<>(
                        getUri(),
                        new ViewUpdatedPayload<>(
                                mapView(newState)
                        )
                )
        );
    }

    protected String getUri() {
        return this.getClass()
                   .getSimpleName();
    }
}
