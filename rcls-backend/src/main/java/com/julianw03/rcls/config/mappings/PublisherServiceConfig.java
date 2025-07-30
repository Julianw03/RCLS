package com.julianw03.rcls.config.mappings;

import com.julianw03.rcls.service.base.publisher.PublisherService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.publisher-service")
public class PublisherServiceConfig {
    private List<PublisherService.Source> disabledSources;
}
