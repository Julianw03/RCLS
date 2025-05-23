package com.julianw03.rcls.service.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.Util.Utils;
import com.julianw03.rcls.config.ServiceConfig;
import com.julianw03.rcls.model.SupportedGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UnixProcessService extends ProcessService {
    private static final Logger log = LoggerFactory.getLogger(UnixProcessService.class);
    private final ObjectMapper mapper;
    private       Path         rcsPath;

    public UnixProcessService(OperatingSystem os, ServiceConfig.ProcessServiceConfig config) {
        super(os, config);
        mapper = new ObjectMapper();
    }

    @Override
    protected Optional<Path> getRiotClientServicesExecutablePath() {
        Path programFilesPath = Paths.get("/Users/Shared");
        Path riotGamesPath = programFilesPath.resolve(config.getSharedComponents().getRiotGamesFolderName());

        File riotGamesFolder = riotGamesPath.toFile();
        if (!riotGamesFolder.exists() || !riotGamesFolder.isDirectory()) {
            log.error("The Riot Games folder does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        Path riotClientInstallsPath = riotGamesPath.resolve(config.getSharedComponents().getRiotClientInstallsFile());
        File riotClientInstallsFile = riotClientInstallsPath.toFile();

        if (!riotClientInstallsFile.exists() || !riotClientInstallsFile.isFile()) {
            log.error("The Riot Client Installs file does not exist, unable to find the Riot Client Services executable");
            return Optional.empty();
        }

        //Get stream from file
        try (InputStream is = Files.newInputStream(riotClientInstallsFile.toPath())) {
            Optional<String> optJsonString = Utils.inputStreamToString(is);
            final String riotClientInstalls = config.getSharedComponents().getRiotClientInstallsFile();
            if (optJsonString.isEmpty()) {
                log.error("Unable to read the {} file", riotClientInstalls);
                return Optional.empty();
            }

            String jsonString = optJsonString.get();
            if (jsonString.trim().isEmpty()) {
                log.error("{} is empty!", riotClientInstalls);
                return Optional.empty();
            }

            Optional<JsonNode> optJsonNode = Utils.parseJson(mapper, jsonString);
            if (optJsonNode.isEmpty()) {
                log.error("The {} cannot be parsed as Json", riotClientInstalls);
                return Optional.empty();
            }

            JsonNode jsonElement = optJsonNode.get();
            if (!jsonElement.isObject()) {
                log.error("The {} does not contain a Json object", riotClientInstalls);
                return Optional.empty();
            }

            ObjectNode objectNode = (ObjectNode) jsonElement;
            String[] useKeys = new String[]{"rc_live", "rc_default"};
            JsonNode useNode = null;
            for (String lookupKey : useKeys) {
                if ((useNode = objectNode.get(lookupKey)) != null && !useNode.isNull()) break;
                log.info("Lookup for key {} failed", lookupKey);
            }

            if (useNode == null) {
                log.error("Unable to get RCS Path, all lookups failed");
                return Optional.empty();
            }

            String stringRCSPath = useNode.asText();
            if (stringRCSPath == null || stringRCSPath.isEmpty()) {
                log.error("The Riot Client Services path is empty");
                return Optional.empty();
            }

            Path rcsPath = Paths.get(stringRCSPath);
            File rcsFile = rcsPath.toFile();
            if (!rcsFile.exists() || !rcsFile.isFile()) {
                log.error("The Riot Client Services file does not exist");
                return Optional.empty();
            }

            return Optional.of(rcsPath);
        } catch (Exception e) {
            log.error("Failed to get RCS Path", e);
        }
        return Optional.empty();
    }
}
