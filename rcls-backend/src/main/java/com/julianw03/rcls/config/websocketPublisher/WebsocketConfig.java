package com.julianw03.rcls.config.websocketPublisher;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "custom.configurations.publishing", ignoreInvalidFields = true)
public class WebsocketConfig {
    private WebsocketPublishingFormat publishingFormat = WebsocketPublishingFormat.RAW_JSON;
}
