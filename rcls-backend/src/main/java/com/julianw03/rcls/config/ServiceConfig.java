package com.julianw03.rcls.config;

import com.julianw03.rcls.config.mappings.OperatingSystemProviderConfig;
import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.config.mappings.ProcessServiceConfig;
import com.julianw03.rcls.config.mappings.RiotClientServiceConfig;
import com.julianw03.rcls.service.base.cacheService.CacheService;
import com.julianw03.rcls.providers.os.OperatingSystemProvider;
import com.julianw03.rcls.providers.paths.PathProvider;
import com.julianw03.rcls.service.base.process.ProcessService;
import com.julianw03.rcls.service.base.publisher.PublisherService;
import com.julianw03.rcls.service.base.publisher.PublisherServiceImpl;
import com.julianw03.rcls.service.base.publisher.formats.ProxyFormat;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.base.riotclient.RiotClientServiceImpl;
import com.julianw03.rcls.service.base.riotclient.connection.ConnectionStrategy;
import com.julianw03.rcls.service.base.riotclient.connection.LockfileConnectionStrategy;
import com.julianw03.rcls.service.base.riotclient.connection.ProcessTakeoverConnectionStrategy;
import com.julianw03.rcls.service.base.riotclient.connection.RiotClientConnectionStrategy;
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
        ConnectionStrategy strategy = riotClientServiceConfig.getConnectionStrategy().getStrategy();
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
        return new ProcessService(
                pathProvider,
                ProcessHandle::allProcesses,
                processServiceConfig
        );
    }


    @Bean
    public RiotClientService getRiotclientService(
            @Autowired ProcessService processService,
            @Autowired PublisherService publisherService,
            @Autowired CacheService cacheService,
            @Autowired RiotClientServiceConfig riotClientServiceConfig,
            @Autowired RiotClientConnectionStrategy connectionStrategy
    ) {

        RiotClientService riotClientService = new RiotClientServiceImpl(
                processService,
                connectionStrategy,
                riotClientServiceConfig
        );
        riotClientService.addMessageListener(message -> {
            publisherService.dispatchChange(PublisherService.Source.PROXY_SERVICE, new ProxyFormat(message));
        });
        riotClientService.addMessageListener(cacheService);
        return riotClientService;
    }

    @Bean
    public PublisherService getPublisherService() {
        return new PublisherServiceImpl();
    }
}
