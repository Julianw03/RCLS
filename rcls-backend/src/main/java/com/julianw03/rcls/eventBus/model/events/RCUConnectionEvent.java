package com.julianw03.rcls.eventBus.model.events;

public record RCUConnectionEvent(
        boolean isConnected
) implements SimpleEvent<Boolean>{

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public Boolean getPayload() {
        return isConnected;
    }
}
