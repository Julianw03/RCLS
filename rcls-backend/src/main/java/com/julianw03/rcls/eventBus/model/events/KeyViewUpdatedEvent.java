package com.julianw03.rcls.eventBus.model.events;

public class KeyViewUpdatedEvent<K, V> extends SimpleEvent<KeyViewUpdatedPayload<K, V>> {

    private String source;
    private KeyViewUpdatedPayload<K, V> payload;
    private String payloadType;

    public KeyViewUpdatedEvent(
            String source,
            KeyViewUpdatedPayload<K, V> payload
    ) {
        super(source, payload);
    }
}
