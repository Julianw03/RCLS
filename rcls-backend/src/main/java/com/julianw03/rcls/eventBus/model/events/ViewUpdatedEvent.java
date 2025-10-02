package com.julianw03.rcls.eventBus.model.events;

public class ViewUpdatedEvent<T> extends SimpleEvent<ViewUpdatedPayload<T>> {

    public ViewUpdatedEvent(
            String source,
            ViewUpdatedPayload<T> payload
    ) {
        super(
                source,
                payload
        );
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public ViewUpdatedPayload<T> getPayload() {
        return payload;
    }
}
