package com.julianw03.rcls.service.base.publisher;

import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;

public record PublisherMessage<T extends PublisherFormat>(String service, String type, T data) {}
