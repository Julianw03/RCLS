package com.julianw03.rcls.eventBus.model;

import com.julianw03.rcls.eventBus.model.events.SimpleEvent;
import reactor.core.publisher.Flux;

public interface MultiChannelBus {
    <T extends SimpleEvent<?>> void publish(
            Channel channel,
            T event
    );

    <T extends SimpleEvent<?>> Flux<T> getFlux(
            Channel channel,
            Class<T> eventType
    );

    Flux<SimpleEvent<?>> getFlux(
            Channel channel
    );
}
