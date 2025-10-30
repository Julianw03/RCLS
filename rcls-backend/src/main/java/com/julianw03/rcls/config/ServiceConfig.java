package com.julianw03.rcls.config;

import com.julianw03.rcls.config.mappings.OperatingSystemProviderConfig;
import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.config.mappings.ProcessServiceConfig;
import com.julianw03.rcls.config.mappings.RiotClientServiceConfig;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.providers.os.OperatingSystemProvider;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.process.ProcessService;
import com.julianw03.rcls.service.process.ProcessServiceImpl;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import com.julianw03.rcls.service.riotclient.RiotClientServiceImpl;
import com.julianw03.rcls.service.riotclient.connection.ConnectionStrategy;
import com.julianw03.rcls.service.riotclient.connection.LockfileConnectionStrategy;
import com.julianw03.rcls.service.riotclient.connection.ProcessTakeoverConnectionStrategy;
import com.julianw03.rcls.service.riotclient.connection.RiotClientConnectionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ServiceConfig {
    @Bean
    PathProvider getPathProvider(
            @Autowired PathProviderConfig pathProviderConfig,
            @Autowired OperatingSystemProvider operatingSystemProvider
    ) {
        return new PathProvider(
                pathProviderConfig,
                operatingSystemProvider.get()
        );
    }

    @Bean
    OperatingSystemProvider getOperatingSystemProvider(
            @Autowired OperatingSystemProviderConfig operatingSystemProviderConfig
    ) {
        return new OperatingSystemProvider(operatingSystemProviderConfig);
    }

    @Bean
    RiotClientConnectionStrategy getRiotClientConnectionStrategy(
            @Autowired ProcessService processService,
            @Autowired PathProvider pathProvider,
            @Autowired RiotClientServiceConfig riotClientServiceConfig
    ) {
        ConnectionStrategy strategy = riotClientServiceConfig.getConnectionStrategy()
                                                             .getStrategy();
        final RiotClientConnectionStrategy connectionStrategy;
        switch (strategy) {
            case LOCKFILE -> connectionStrategy = new LockfileConnectionStrategy(pathProvider);
            case PROCESS_TAKEOVER -> connectionStrategy = new ProcessTakeoverConnectionStrategy(processService);
            default -> {
                log.error("Unknown connection strategy: " + strategy);
                throw new IllegalStateException("Unknown connection strategy: " + strategy);
            }
        }
        return connectionStrategy;
    }

    @Bean
    public ProcessService getProcessService(
            @Autowired ProcessServiceConfig processServiceConfig,
            @Autowired PathProvider pathProvider
    ) {
        return new ProcessServiceImpl(
                pathProvider,
                ProcessHandle::allProcesses,
                processServiceConfig
        );
    }


    @Bean
    public RiotClientService getRiotclientService(
            @Autowired MultiChannelBus eventBus,
            @Autowired RiotClientServiceConfig riotClientServiceConfig,
            @Autowired RiotClientConnectionStrategy connectionStrategy
    ) {

        RiotClientService riotClientService = new RiotClientServiceImpl(
                connectionStrategy,
                eventBus,
                riotClientServiceConfig
        );
        return riotClientService;
    }

}
