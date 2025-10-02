package com.julianw03.rcls.eventBus.model.events;

import com.julianw03.rcls.model.RCUWebsocketMessage;

public class RCUMessageEvent extends SimpleEvent<RCUWebsocketMessage> {
    public RCUMessageEvent(
            String source,
            RCUWebsocketMessage payload
    ) {
        super(
                source,
                payload
        );
    }
}
