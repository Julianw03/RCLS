package com.julianw03.rcls.eventBus.model.events;

public class RCUConnectionEvent extends SimpleEvent<Boolean> {

    public RCUConnectionEvent(
            String source,
            Boolean isConnected
    ) {
        super(
                source,
                isConnected
        );
    }
}
