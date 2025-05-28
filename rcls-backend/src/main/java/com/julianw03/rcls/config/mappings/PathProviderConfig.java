package com.julianw03.rcls.config.mappings;

import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.base.process.OperatingSystem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.path-provider", ignoreInvalidFields = true)
public class PathProviderConfig {
    private Map<OperatingSystem, PathEntries> pathEntries;
    @Data
    public static class PathEntries {
        @Data
        public static class Executables {
            private Map<SupportedGame, String> gameExecutables;
            private String                     riotClient;
            private String                     riotClientServices;
        }

        private Executables executables;
        private String      programFilesPath;
        private String      riotGamesFolderName;
        private String      riotClientInstallsFile;
        private String      riotClientLockFileLocation;
    }
}
