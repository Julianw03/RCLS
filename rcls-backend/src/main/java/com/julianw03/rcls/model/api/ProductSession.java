package com.julianw03.rcls.model.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class ProductSession {
    private Integer exitCode;
    private TerminationReason exitReason;
    private Boolean isInternal;
    private LaunchConfiguration launchConfiguration;
    private String patchlineFullName;
    private String patchlineId;
    private ProductPhase phase;
    private String productId;
    private String version;

    public enum TerminationReason {
        StillRunning,
        Interrupt,
        Exit,
        Timeout,
        Unknown
    }

    public enum ProductPhase {
        None,
        Pending,
        Idle,
        Gameplay
    }

    @Getter
    @NoArgsConstructor
    public static class LaunchConfiguration {
        private List<String> arguments;
        private String executable;
        private String locale;
        private String voiceLocale;
        private String workingDirectory;
    }
}
