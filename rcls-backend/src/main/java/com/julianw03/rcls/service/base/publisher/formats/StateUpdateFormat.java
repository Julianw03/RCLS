package com.julianw03.rcls.service.base.publisher.formats;

public record StateUpdateFormat<T>(
        String uri,
        T state
) implements PublisherFormat {
}
