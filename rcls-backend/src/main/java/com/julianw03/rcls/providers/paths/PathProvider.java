package com.julianw03.rcls.providers.paths;

import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.service.process.OperatingSystem;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.function.Supplier;


@Slf4j
public class PathProvider implements Supplier<PathProviderConfig.PathEntries> {

    protected final PathProviderConfig config;
    protected final OperatingSystem    os;

    public PathProvider(
            PathProviderConfig config,
            OperatingSystem os
    ) {
        if (config == null) {
            throw new IllegalArgumentException("PathProviderConfig cannot be null");
        }
        if (os == null) {
            throw new IllegalArgumentException("OperatingSystem cannot be null");
        }
        this.config = config;
        this.os = os;
    }

    @Override
    public PathProviderConfig.PathEntries get() {
        return config.getPathEntries()
                     .get(os);
    }

    public PathProviderConfig.SharedEntries getSharedEntries() {
        return config.getSharedEntries();
    }

    public Path getBaseConfigPath() {
        return Path.of(
                           this.get()
                               .getConfigBasePath())
                   .resolve(
                           this.getSharedEntries()
                               .getApplicationFolderName()
                   );
    }
}
