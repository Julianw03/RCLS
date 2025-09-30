package com.julianw03.rcls.config.mappings;

import com.julianw03.rcls.service.process.OperatingSystem;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.operating-system-provider", ignoreInvalidFields = true)
public class OperatingSystemProviderConfig {
    private OperatingSystem osOverride;
}
