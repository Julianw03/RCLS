package com.julianw03.rcls.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;

public record PublisherMessage(Type type, String uri, JsonNode data) {
    public enum Type {
        CREATE,
        UPDATE,
        DELETE
    }
}
