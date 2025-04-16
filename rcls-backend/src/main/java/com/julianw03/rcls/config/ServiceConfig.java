package com.julianw03.rcls.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.process.SupportedOperatingSystem;
import com.julianw03.rcls.service.process.WindowsProcessService;
import com.julianw03.rcls.service.publisher.PublisherMessage;
import com.julianw03.rcls.service.publisher.PublisherService;
import com.julianw03.rcls.service.publisher.PublisherServiceImpl;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.RiotClientServiceImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

@Slf4j
@Configuration
public class ServiceConfig {
    @Bean
    public ProcessService getProcessService(
            @Autowired ProcessServiceConfig processServiceConfig
    ) {
        final SupportedOperatingSystem os = SupportedOperatingSystem
                .fromName(System.getProperty("os.name"))
                .orElseThrow();
        if (os == SupportedOperatingSystem.WINDOWS) {
            return new WindowsProcessService(processServiceConfig);
        }

        throw new RuntimeException("Operating System not yet supported");
    }


    @Bean
    @DependsOn({"getProcessService", "getPublisherService", "cacheService"})
    public RiotClientService getRiotclientService(
            @Autowired ProcessService processService,
            @Autowired PublisherService publisherService,
            @Autowired CacheService cacheService
            ) {
        RiotClientService riotClientService = new RiotClientServiceImpl(processService);
        riotClientService.addMessageListener(message -> {
            PublisherMessage.Type type;
            switch (message.getType()) {
                case CREATE:
                    type = PublisherMessage.Type.CREATE;
                    break;
                case UPDATE:
                    type = PublisherMessage.Type.UPDATE;
                    break;
                case DELETE:
                    type = PublisherMessage.Type.DELETE;
                    break;
                default:
                    log.debug("Type {} not initialized", message.getType());
                    return;
            }

            publisherService.doDispatchChange(type, "/rcls-proxy" + message.getUri(), message.getData());
        });
        riotClientService.addMessageListener((message) -> {
            log.info("{} - {}: {}", message.getType(), message.getUri(), message.getData());
        });
        riotClientService.addMessageListener(cacheService);
        return riotClientService;
    }

    @Bean
    public PublisherService getPublisherService() {
        return new PublisherServiceImpl();
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "process-service")
    public static class ProcessServiceConfig {
        private Map<SupportedOperatingSystem, OSExecutableMappings> executables;
        private SharedComponents                                    sharedComponents;
        private ConnectionInitParameters                            connectionInit;

        @Data
        public static class SharedComponents {
            private String riotGamesFolderName;
            private String riotClientInstallsFile;
        }

        @Data
        public static class ConnectionInitParameters {
            private int processSearchAttempts;
            private int processSearchDelayMs;
            private int processSearchDurationMaxMs;
            private int restConnectAttempts;
            private int restConnectDelayMs;
            private int restConnectDurationMaxMs;
        }

        @Data
        public static class OSExecutableMappings {
            private Map<SupportedGame, String> gameExecutables;
            private String                     riotClient;
            private String                     riotClientServices;
        }
    }
}
