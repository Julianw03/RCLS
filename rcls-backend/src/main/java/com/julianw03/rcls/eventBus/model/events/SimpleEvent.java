package com.julianw03.rcls.eventBus.model.events;

public interface SimpleEvent<T> {
    String getSource();

    T getPayload();
}
