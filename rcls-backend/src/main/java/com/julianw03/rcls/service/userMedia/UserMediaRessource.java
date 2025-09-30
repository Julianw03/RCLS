package com.julianw03.rcls.service.userMedia;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class UserMediaRessource {
    private Resource resource;
    private String contentType;
}
