package com.julianw03.rcls.eventBus.model.events;

import lombok.Getter;

import java.util.Optional;

@Getter
public abstract class SimpleEvent<T> {

    protected String source;
    protected T      payload;
    protected String payloadType;

    protected SimpleEvent(
            String source,
            T payload
    ) {
        this.source = source;
        this.payload = payload;
        this.payloadType = getPayloadType(payload);
    }

    protected String getPayloadType(T payload) {
        return Optional.ofNullable(payload)
                       .map(Object::getClass)
                       .map(Class::getSimpleName)
                       .orElse("null");
    }
}
