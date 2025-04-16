package com.julianw03.rcls.model;

public interface RCUMessageListener {
    default void onConnect() {};
    void onMessage(RCUWebsocketMessage message);
    default void onDisconnect() {}
}
