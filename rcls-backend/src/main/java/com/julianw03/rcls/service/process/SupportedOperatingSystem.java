package com.julianw03.rcls.service.process;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public enum SupportedOperatingSystem {
    WINDOWS((osName) -> osName.contains("windows")),
    LINUX((osName) -> osName.contains("linux")),
    MACOS((osName) -> osName.contains("mac") || osName.contains("darwin"));

    SupportedOperatingSystem(Function<String, Boolean> operatingSystemCheck) {
        this.operatingSystemCheck = operatingSystemCheck;
    }

    private final Function<String, Boolean> operatingSystemCheck;

    public static Optional<SupportedOperatingSystem> fromName(String providedOsName) {
        if (providedOsName == null) {
            return Optional.empty();
        }
        final String osName = providedOsName.toLowerCase();
        log.info("Attempting to match for Operating System: {}", providedOsName);
        return Arrays.stream(SupportedOperatingSystem.values())
                .filter(os -> {
                    log.info("Checking if Operating System {} matches", os);
                    return os.operatingSystemCheck.apply(osName);
                })
                .findFirst();
    }
}