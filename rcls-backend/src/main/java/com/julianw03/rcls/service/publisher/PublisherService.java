package com.julianw03.rcls.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.julianw03.rcls.service.publisher.PublisherMessage.Type;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Flow;

@Slf4j
@Service
public abstract class PublisherService extends TextWebSocketHandler implements Flow.Publisher<PublisherMessage> {
    @PostConstruct
    protected abstract void init();

    @PreDestroy
    protected abstract void destroy();

    public void dispatchChange(
            Type type,
            String uri,
            JsonNode data
    ) {
        try {
            this.doDispatchChange(type, uri, data);
        } catch (Exception e) {
            log.error("Failed to dispatch change!", e);
        }
    }

    public abstract void doDispatchChange(
            Type type,
            String uri,
            JsonNode data
    );

    public void dispatchResourceCreated(
            String uri,
            JsonNode data
    ) {
        this.dispatchChange(Type.CREATE, uri, data);
    }

    public void dispatchResourceUpdated(
            String uri,
            JsonNode data
    ) {
        this.dispatchChange(Type.UPDATE, uri, data);
    }

    public void dispatchResourceDeleted(
            String uri
    ) {
        this.dispatchChange(Type.DELETE, uri, null);
    }
}
