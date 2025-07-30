package com.julianw03.rcls.providers.paths;

import com.julianw03.rcls.config.mappings.PathProviderConfig;
import com.julianw03.rcls.service.base.process.OperatingSystem;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;
import java.util.regex.Pattern;


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
        return config.getPathEntries().get(os);
    }
}
