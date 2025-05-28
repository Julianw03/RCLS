package com.julianw03.rcls.service.base.publisher.formats;

public record MapKeyFormat<K, V>(
        String uri,
        K key,
        V value
) implements PublisherFormat {}
