package com.julianw03.rcls.config;

import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.cacheService.CacheService;
import com.julianw03.rcls.service.process.OperatingSystem;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.process.UnixProcessService;
import com.julianw03.rcls.service.process.WindowsProcessServiceV2;
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

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Configuration
public class ServiceConfig {
    @Bean
    public ProcessService getProcessService(
            @Autowired ProcessServiceConfig processServiceConfig
    ) {
        final String osName = System.getProperty("os.name");
        final OperatingSystem os = Optional
                .ofNullable(processServiceConfig.osOverride)
                .orElseGet(() -> OperatingSystem
                        .fromName(osName)
                        .orElseThrow(
                                () -> new NoSuchElementException(
                                        String.format("Operating System %s not supported", osName)
                                )
                        ));

        switch (os) {
            case WINDOWS -> {
                return new WindowsProcessServiceV2(processServiceConfig);
            }
            case MACOS, LINUX -> {
                return new UnixProcessService(os, processServiceConfig);
            }
            default -> {
                throw new RuntimeException("Operating System not yet supported");
            }
        }
    }


    @Bean
    @DependsOn({"getProcessService", "getPublisherService", "cacheService"})
    public RiotClientService getRiotclientService(
            @Autowired ProcessService processService,
            @Autowired PublisherService publisherService,
            @Autowired CacheService cacheService,
            @Autowired RiotClientServiceConfig riotClientServiceConfig
    ) {
        RiotClientService riotClientService = new RiotClientServiceImpl(
                processService,
                riotClientServiceConfig
        );
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
        riotClientService.addMessageListener(cacheService);
        return riotClientService;
    }

    @Bean
    public PublisherService getPublisherService() {
        return new PublisherServiceImpl();
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "custom.configurations.riotclient-service", ignoreInvalidFields = true)
    public static class RiotClientServiceConfig {
        private ConnectionInitParameters connectionInit;

        @Data
        public static class ConnectionInitParameters {
            private int restConnectAttempts;
            private int restConnectDelayMs;
            private int restConnectWaitForMaxMs;
        }
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "custom.configurations.process-service", ignoreInvalidFields = true)
    public static class ProcessServiceConfig {
        private OperatingSystem                            osOverride;
        private Map<OperatingSystem, OSExecutableMappings> executables;
        private SharedComponents                           sharedComponents;


        @Data
        public static class SharedComponents {
            private String riotGamesFolderName;
            private String riotClientInstallsFile;
        }

        @Data
        public static class OSExecutableMappings {
            private Map<SupportedGame, String> gameExecutables;
            private String                     riotClient;
            private String                     riotClientServices;
        }
    }
}
