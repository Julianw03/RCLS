package com.julianw03.rcls.service.modules.login.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationStateDTO {
    @JsonSetter(nulls = Nulls.SKIP)
    @Builder.Default
    private LoginStatusDTO     loginStatus     = LoginStatusDTO.UNKNOWN;
    @JsonSetter(nulls = Nulls.SET)
    private HCaptchaDTO        hcaptchaStatus;
    @JsonSetter(nulls = Nulls.SET)
    private MultifactorInfoDTO multifactorInfo;
    @JsonSetter(nulls = Nulls.SET)
    private String             countryCode;
}
