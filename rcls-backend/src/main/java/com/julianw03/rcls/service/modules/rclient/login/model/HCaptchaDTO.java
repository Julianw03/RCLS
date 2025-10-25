package com.julianw03.rcls.service.modules.rclient.login.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HCaptchaDTO {
    private String data;
    private String key;
}
