package com.julianw03.rcls.providers.os;

import com.julianw03.rcls.config.mappings.OperatingSystemProviderConfig;
import com.julianw03.rcls.service.process.OperatingSystem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Getter
public class OperatingSystemProvider implements Supplier<OperatingSystem> {

    private       OperatingSystem               operatingSystem;
    private final OperatingSystemProviderConfig config;

    public OperatingSystemProvider(
            OperatingSystemProviderConfig config
    ) {
        this.config = config;
    }

    @Override
    public OperatingSystem get() {
        if (operatingSystem == null) {
            final String osName = System.getProperty("os.name").toLowerCase();

            operatingSystem = Optional
                    .ofNullable(config.getOsOverride())
                    .orElseGet(() -> {
                        log.debug("OS Override not set, using system property");
                        return OperatingSystem
                                .fromName(osName)
                                .orElseThrow(
                                        () -> new NoSuchElementException(
                                                String.format("Operating System %s not supported", osName)
                                        )
                                );
                    });


            log.info("Using OS Config for {}", operatingSystem.name());
        }


        return operatingSystem;
    }
}
