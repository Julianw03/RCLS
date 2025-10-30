package com.julianw03.rcls.service.modules.standalone.userMedia;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class UserMediaRessource {
    private Resource resource;
    private String contentType;
}
