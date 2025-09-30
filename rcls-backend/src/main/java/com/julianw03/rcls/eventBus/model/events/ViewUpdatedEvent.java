package com.julianw03.rcls.eventBus.model.events;

public record ViewUpdatedEvent<T>(
        String source,
        ViewUpdatedPayload<T> payload
) implements SimpleEvent<ViewUpdatedPayload<T>> {
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public ViewUpdatedPayload<T> getPayload() {
        return payload;
    }
}
