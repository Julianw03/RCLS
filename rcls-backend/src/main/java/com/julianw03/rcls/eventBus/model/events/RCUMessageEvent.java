package com.julianw03.rcls.eventBus.model.events;

import com.julianw03.rcls.model.RCUWebsocketMessage;

public record RCUMessageEvent(
        String source,
        RCUWebsocketMessage payload
) implements SimpleEvent<RCUWebsocketMessage> {
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public RCUWebsocketMessage getPayload() {
        return payload;
    }
}
