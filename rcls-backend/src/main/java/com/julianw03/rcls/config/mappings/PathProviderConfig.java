package com.julianw03.rcls.config.mappings;

import com.julianw03.rcls.model.PluginModuleType;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.service.process.OperatingSystem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "custom.configurations.path-provider", ignoreInvalidFields = true)
public class PathProviderConfig {
    private Map<OperatingSystem, PathEntries> pathEntries;
    private SharedEntries                     sharedEntries;


    @Data
    public static class SharedEntries {
        private Map<PluginModuleType, String> pluginFolderNames;
        private String                        applicationFolderName;
    }

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
        private String      configBasePath;
        private String      riotGamesFolderName;
        private String      riotClientInstallsFile;
        private String      riotClientLockFileLocation;
    }
}
