package com.julianw03.rcls.service.base.publisher.formats;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.model.RCUWebsocketMessage;

public record ProxyFormat(
        RCUWebsocketMessage.MessageType type,
        String uri,
        JsonNode data
) implements PublisherFormat {
    public ProxyFormat(
            RCUWebsocketMessage message
    ) {
        this(
                message.getType(),
                message.getUri(),
                message.getData()
        );
    }
}
