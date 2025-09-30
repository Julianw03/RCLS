package com.julianw03.rcls.config.mappings;

import com.julianw03.rcls.service.riotclient.connection.ConnectionStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.riotclient-service")
public class RiotClientServiceConfig {
    private ConnectionInitParameters connectionInit;
    private ConnectionStrategyParams connectionStrategy;

    @Data
    public static class ConnectionStrategyParams {
        private ConnectionStrategy strategy;
        private int                connectTimeoutMs;
    }

    @Data
    public static class ConnectionInitParameters {
        private int restConnectAttempts;
        private int restConnectDelayMs;
        private int restConnectWaitForMaxMs;
    }
}
