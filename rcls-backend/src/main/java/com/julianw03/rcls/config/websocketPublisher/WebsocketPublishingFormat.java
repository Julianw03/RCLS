package com.julianw03.rcls.config.websocketPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.web.socket.AbstractWebSocketMessage;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import java.util.function.Supplier;

public enum WebsocketPublishingFormat {
    RAW_JSON,
    MSGPACK;

    public Supplier<ObjectMapper> getMapperFactory() {
        return switch (this) {
            case RAW_JSON -> ObjectMapper::new;
            case MSGPACK -> () -> new ObjectMapper(new MessagePackFactory());
        };
    }

    public AbstractWebSocketMessage<?> createMessage(byte[] payload) {
        return switch (this) {
            case RAW_JSON -> new TextMessage(payload);
            case MSGPACK -> new BinaryMessage(payload);
        };
    }
}
