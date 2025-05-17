package com.julianw03.rcls.config.mappings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.process-service", ignoreInvalidFields = true)
public class ProcessServiceConfig {
    private SharedComponents sharedComponents;

    @Data
    public static class SharedComponents {
        private String riotGamesFolderName;
        private String riotClientInstallsFile;
    }
}
