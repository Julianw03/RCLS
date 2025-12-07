package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedPayload;
import com.julianw03.rcls.model.services.ServiceDTO;
import com.julianw03.rcls.model.services.ServiceSerializable;
import com.julianw03.rcls.service.riotclient.RiotClientService;

import java.util.Objects;

/**
 *
 * @param <T> The type of updates that are received from the Riot Client. Usually an auto-generated model.
 * @param <V> The type of the View that is being exposed to other modules. This view can (and should) be the most ideomatic representation of the data.
 *            This being a separate type from {@link P} also allows for better state management via, for example, sealed classes.
 * @param <P> The type of the Publishing View that is being sent via the Event Bus. This view should be optimized for serialization and transmission and should be OpenAPI-compliant.
 *            Sealed classes do not work that well so having a separate DTO may be useful.
 *
 **/
public abstract class PublishingObjectDataManager<T, V extends ServiceSerializable, P extends ServiceDTO<V>> extends ObjectDataManager<T, V> {
    protected PublishingObjectDataManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
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
        final P newPubView = mapPublishingView(newState);
        if (Objects.equals(
                mapPublishingView(prevState),
                newPubView
        )) {
            return;
        }
        eventBus.publish(
                Channel.DATA_MANAGER,
                new ViewUpdatedEvent<>(
                        getUri(),
                        new ViewUpdatedPayload<>(
                                mapPublishingView(newState)
                        )
                )
        );
    }

    protected abstract P mapPublishingView(T state);

    protected String getUri() {
        return this.getClass()
                   .getSimpleName();
    }
}
