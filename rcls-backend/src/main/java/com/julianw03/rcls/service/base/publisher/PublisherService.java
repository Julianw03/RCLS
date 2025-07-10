package com.julianw03.rcls.service.base.publisher;

import com.julianw03.rcls.config.mappings.PublisherServiceConfig;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
public abstract class PublisherService extends BinaryWebSocketHandler {
    protected final PublisherServiceConfig config;

    protected PublisherService (PublisherServiceConfig config) {
        super();
        this.config = config;
    }

    @PostConstruct
    protected abstract void init();

    @PreDestroy
    protected abstract void destroy();

    public enum Source {
        RCU_STATE_SERVICE,
        RCLS_STATE_SERVICE,
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
        if (config.getDisabledSources() != null && config.getDisabledSources().contains(source)) {
            return;
        }
        try {
            this.doDispatchChange(source.name(), data.getClass().getSimpleName(), data);
        } catch (Exception e) {
            log.error("Failed to dispatch change!", e);
        }
    }

    protected abstract void doDispatchChange(
            String source,
            String dataType,
            PublisherFormat format
    );
}
