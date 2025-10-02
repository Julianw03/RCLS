package com.julianw03.rcls.service.modules.login.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class HCaptchaDTO {
    private String data;
    private String key;
}
