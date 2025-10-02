package com.julianw03.rcls.eventBus.model.events;

public record KeyViewUpdatedPayload<K, V>(K key, V newValue) {}
