package com.julianw03.rcls.model.data;

import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.KeyViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.KeyViewUpdatedPayload;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedEvent;
import com.julianw03.rcls.eventBus.model.events.ViewUpdatedPayload;
import com.julianw03.rcls.model.services.ServiceDTO;
import com.julianw03.rcls.model.services.ServiceSerializable;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.jetbrains.annotations.Contract;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class PublishingMapDataManager<K, V, E extends ServiceSerializable, P extends ServiceDTO<E>> extends MapDataManager<K, V, E> {

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
        super.onKeyUpdated(
                key,
                previousValue,
                newValue
        );
        final P newPubValueView = mapPublishingValueView(newValue);
        if (Objects.equals(
                mapPublishingValueView(previousValue),
                newPubValueView
        )) {
            return;
        }
        eventBus.publish(
                Channel.DATA_MANAGER,
                new KeyViewUpdatedEvent<>(
                        getUri(),
                        new KeyViewUpdatedPayload<>(
                                key,
                                newPubValueView
                        )
                )
        );
    }

    @Override
    protected void onStateUpdated(
            Map<K, V> previousState,
            Map<K, V> newState
    ) {
        super.onStateUpdated(
                previousState,
                newState
        );
        final Map<K, P> newPubView = mapPublishingView(newState);
        if (Objects.equals(
                mapPublishingView(previousState),
                newPubView
        )) {
            return;
        }
        eventBus.publish(
                Channel.DATA_MANAGER,
                new ViewUpdatedEvent<>(
                        getUri(),
                        new ViewUpdatedPayload<>(
                                newPubView
                        )
                )
        );
    }

    protected String getUri() {
        return this.getClass()
                   .getSimpleName();
    }

    @Contract(pure = true)
    protected abstract P mapPublishingValueView(V state);

    @Contract(pure = true)
    protected Map<K, P> mapPublishingView(Map<K, V> map) {
        return map.entrySet()
                  .stream()
                  .map(entry -> {
                      K key = entry.getKey();
                      V value = entry.getValue();
                      return Map.entry(
                              key,
                              mapPublishingValueView(value)
                      );
                  })
                  .collect(Collectors.toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue
                  ));
    }
}
