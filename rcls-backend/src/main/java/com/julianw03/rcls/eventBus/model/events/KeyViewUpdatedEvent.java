package com.julianw03.rcls.eventBus.model.events;

public record KeyViewUpdatedEvent<K, V>(
        String source,
        KeyViewUpdatedPayload<K, V> payload
) implements SimpleEvent<KeyViewUpdatedPayload<K, V>> {
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public KeyViewUpdatedPayload<K, V> getPayload() {
        return payload;
    }
}
