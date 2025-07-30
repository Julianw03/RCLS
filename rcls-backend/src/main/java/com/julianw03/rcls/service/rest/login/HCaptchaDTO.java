package com.julianw03.rcls.service.rest.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HCaptchaDTO {
    private String data;
    private String key;
}
