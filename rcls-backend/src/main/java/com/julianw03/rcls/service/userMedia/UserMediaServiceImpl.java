package com.julianw03.rcls.service.userMedia;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.providers.paths.PathProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserMediaServiceImpl extends UserMediaService {
    private static final Pattern MEDIA_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+/([a-zA-Z0-9\\-+.]+)$");
    private final PathProvider pathProvider;
    private final ObjectMapper mapper;
    private Path basePath;

    public UserMediaServiceImpl(
            @Autowired PathProvider pathProvider
    ) {
        this.pathProvider = pathProvider;
        this.mapper = new ObjectMapper();
    }

    @Override
    protected void startup() {
        super.startup();
        setupAndEnsureBaseFilePaths();
    }

    private void setupAndEnsureBaseFilePaths() {
        Path rootConfigPath = Paths.get(pathProvider.get().getConfigBasePath());
        File rootConfigDir = rootConfigPath.toFile();
        if (!rootConfigDir.exists() || !rootConfigDir.isDirectory()) {
            log.error("The config directory does not exist or is not a directory");
            return;
        }

        log.info("Root User config path exists and is {}", rootConfigPath);


        rootConfigPath = rootConfigPath
                .resolve(pathProvider.getSharedEntries().getApplicationFolderName())
                .resolve(pathProvider.getSharedEntries().getPluginFolderNames().get(this.getModuleType()));
        try {
            Files.createDirectories(rootConfigPath);
        } catch (IOException e) {
            log.error("Could not create directory {}", rootConfigPath);
            return;
        }


        this.basePath = rootConfigPath.normalize();
    }

    @Override
    void storeFile(MultipartFile file) throws UnsupportedFileTypeException, IOException {
        if (basePath == null) {
            return;
        }

        final String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            throw new UnsupportedFileTypeException("The file type is not supported");
        }

        Matcher matcher = MEDIA_TYPE_PATTERN.matcher(contentType);
        if (!matcher.matches()) {
            throw new UnsupportedFileTypeException("The file type is not supported");
        }
        String extension = matcher.group(1);
        MediaType contentMediaType = MediaType.fromExtension(extension).orElseThrow(() -> new UnsupportedFileTypeException("The file type is not supported"));

        UserMediaConfig newConfig = new UserMediaConfig();
        newConfig.setMediaType(contentMediaType);
        newConfig.setContentType(contentType);
        newConfig.setFileName("background." + extension);

        Path configPath = basePath.resolve("config.json");
        try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
            fos.write(mapper.writeValueAsBytes(newConfig));
        } catch (Exception e) {
            log.error("Failed to write config file", e);
            throw new IOException("Failed to write config file", e);
        }

        log.info("File info: {} {}", file.getName(), file.getContentType());

        Path savePath = basePath.resolve(newConfig.getFileName());
        try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
            fos.write(file.getBytes());
        } catch (Exception e) {
            log.error("Failed to write file", e);
        }
    }

    @Override
    UserMediaConfig loadConfig() throws IOException {
        Path configPath = basePath.resolve("config.json");
        File configFile = configPath.toFile();
        if (!configFile.exists() || !configFile.isFile()) {
            log.error("The config file does not exist or is not a file");
            return null;
        }

        UserMediaConfig config;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            config = mapper.readValue(fis, UserMediaConfig.class);
            if (config == null) {
                log.error("The config file is invalid");
                return null;
            }
        } catch (Exception e) {
            throw new IOException("Failed to read config file", e);
        }

        return config;
    }

    public void resetBackground() throws IOException {
        if (basePath == null) {
            return;
        }

        Path configPath = basePath.resolve("config.json");
        File configFile = configPath.toFile();
        if (configFile.exists() && configFile.isFile()) {
            if (!configFile.delete()) {
                log.error("Failed to delete config file");
                throw new IOException("Failed to delete config file");
            }
        }

        File[] files = basePath.toFile().listFiles((dir, name) -> name.startsWith("background."));
        if (files != null) {
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    if (!file.delete()) {
                        log.error("Failed to delete background file {}", file.getName());
                        throw new IOException("Failed to delete background file " + file.getName());
                    }
                }
            }
        }
    }

    @Override
    public UserMediaRessource loadCurrentBackground() throws IllegalArgumentException, IOException {
        UserMediaConfig config = loadConfig();
        Path savePath = resolveEnsureNoPathTraversal(basePath, config.getFileName());
        log.info("Loading background from {}", savePath);
        File saveFile = savePath.toFile();

        if (!saveFile.exists() || !saveFile.isFile()) {
            log.error("The background file does not exist or is not a file");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(saveFile)) {
            byte[] data = fis.readAllBytes();
            ByteArrayResource resource = new ByteArrayResource(data);
            UserMediaRessource res = new UserMediaRessource();
            res.setContentType(config.getContentType());
            res.setResource(resource);
            return res;
        } catch (Exception e) {
            throw new IOException("Failed to load background from " + savePath, e);
        }
    }

    private Path resolveEnsureNoPathTraversal(Path basePath, String relativePath) {
        Path resolvedPath = basePath.resolve(relativePath).normalize();
        ensureNoPathTraversal(basePath, resolvedPath);
        return resolvedPath;
    }

    private void ensureNoPathTraversal(Path basePath, Path resolvedPath) {
        Path normalizedPath = resolvedPath.normalize();
        if (!normalizedPath.toAbsolutePath().startsWith(basePath.toAbsolutePath())) {
            throw new IllegalArgumentException("The provided path is not a valid path");
        }
    }
}

