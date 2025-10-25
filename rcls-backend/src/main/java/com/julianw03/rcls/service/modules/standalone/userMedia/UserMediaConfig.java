package com.julianw03.rcls.service.modules.standalone.userMedia;

import lombok.Data;

@Data
public class UserMediaConfig {
    private MediaType mediaType;
    private String contentType;
    private String fileName;
}
