package com.julianw03.rcls.eventBus.model.events;

public record ViewUpdatedPayload<T> (
        T newState
) {}
