package com.julianw03.rcls.service.userMedia;

import com.julianw03.rcls.model.PluginModuleType;
import com.julianw03.rcls.service.BaseService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public abstract class UserMediaService extends BaseService {
    abstract void storeFile(MultipartFile file) throws UnsupportedFileTypeException, IOException;

    abstract UserMediaConfig loadConfig() throws IOException;

    abstract UserMediaRessource loadCurrentBackground() throws IOException, IllegalArgumentException;

    abstract void resetBackground() throws IOException;

    protected PluginModuleType getModuleType() {
        return PluginModuleType.USER_BACKGROUND_MODULE;
    }
}
