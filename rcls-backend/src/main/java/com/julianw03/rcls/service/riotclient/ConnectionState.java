package com.julianw03.rcls.service.riotclient;

public enum ConnectionState {
    DISCONNECTED,
    WAITING_FOR_PROCESS,
    WAITING_FOR_REST_READY,
    WAITING_FOR_WEBSOCKET_CONNECTION,
    CONNECTED,
    NOT_FOUND_IDLE;
}
