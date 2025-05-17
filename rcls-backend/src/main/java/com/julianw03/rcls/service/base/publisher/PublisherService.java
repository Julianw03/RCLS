package com.julianw03.rcls.service.base.publisher;

import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public abstract class PublisherService extends TextWebSocketHandler {
    @PostConstruct
    protected abstract void init();

    @PreDestroy
    protected abstract void destroy();

    public enum Source {
        CACHE_SERVICE,
        PROXY_SERVICE,
        RC_CONNECTION_STATE
    }

    public void dispatchChange(
            Source source,
            PublisherFormat data
    ) {
        if (source == null || data == null) {
            log.warn("Failed to dispatch change! Source or data is null!");
            return;
        }
        try {
            this.doDispatchChange(source.name(), data.getClass().getSimpleName(), data);
        } catch (Exception e) {
            log.error("Failed to dispatch change!", e);
        }
    }

    protected abstract void doDispatchChange(
            String service,
            String dataType,
            PublisherFormat format
    );
}
