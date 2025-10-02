package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedPayload;
import com.julianw03.rcls.service.riotclient.RiotClientService;

public abstract class PublishingObjectDataManager<T, V> extends ObjectDataManager<T, V> {
    protected PublishingObjectDataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(riotClientService, eventBus);
    }

    @Override
    protected void onStateUpdated(
            T prevState,
            T newState
    ) {
        super.onStateUpdated(
                prevState,
                newState
        );
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
