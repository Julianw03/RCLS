package com.julianw03.rcls.service.websocketPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.config.websocketPublisher.WebsocketConfig;
import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.SimpleEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class WebsocketPublisher extends TextWebSocketHandler {
    private       AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>(null);
    private final ObjectMapper                      mapper;
    private       Disposable                        dataManagerSubscription;
    private       Disposable                        rcConnectionStatusSubscription;
    private final WebsocketConfig                   websocketConfig;

    public WebsocketPublisher(
            MultiChannelBus eventBus,
            WebsocketConfig websocketConfig
    ) {
        this.websocketConfig = websocketConfig;
        this.mapper = websocketConfig.getPublishingFormat()
                                     .getMapperFactory()
                                     .get();
        dataManagerSubscription = eventBus.getFlux(Channel.DATA_MANAGER)
                                          .concatMap(this::sendMessage)
                                          .onErrorContinue((err, obj) -> log.error(
                                                  "Failed to send message: {}",
                                                  obj,
                                                  err
                                          ))
                                          .subscribe();

        rcConnectionStatusSubscription = eventBus.getFlux(Channel.RCU_CONNECTION_STATE)
                                                 .concatMap(this::sendMessage)
                                                 .onErrorContinue((err, obj) -> log.error(
                                                         "Failed to send message: {}",
                                                         obj,
                                                         err
                                                 ))
                                                 .subscribe();

    }

    private Mono<Void> sendMessage(SimpleEvent<?> event) {
        WebSocketSession session = sessionRef.get();
        if (session == null || !session.isOpen()) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            byte[] bytes = mapper.writeValueAsBytes(event);
            session.sendMessage(websocketConfig.getPublishingFormat()
                                               .createMessage(bytes));
            return null;
        });
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRef.set(session);
        log.info(
                "WebSocket connected: {}",
                session.getId()
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        sessionRef.compareAndSet(
                session,
                null
        );
        log.info(
                "WebSocket closed: {} with status {}",
                session.getId(),
                status
        );
    }

    @PreDestroy
    public void destroy() {
        Optional.ofNullable(dataManagerSubscription)
                .ifPresent(Disposable::dispose);

        Optional.ofNullable(rcConnectionStatusSubscription)
                .ifPresent(Disposable::dispose);
    }
}
