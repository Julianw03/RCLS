package com.julianw03.rcls.config.websocketPublisher;

import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.service.websocketPublisher.WebsocketPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebsocketConfigurerConfig implements WebSocketConfigurer {
    private final WebsocketPublisher websocketPublisher;

    @Autowired
    public WebsocketConfigurerConfig(
            MultiChannelBus eventBus,
            WebsocketConfig config
    ) {
        this.websocketPublisher = new WebsocketPublisher(eventBus, config);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(
                        websocketPublisher,
                        "/ws"
                )
                .setAllowedOrigins("*");
    }
}
