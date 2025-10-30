package com.julianw03.rcls.service.modules.rclient.login.model;

import lombok.Data;

@Data
public class MultifactorInputDTO {
    String otp;
    Boolean rememberDevice;
}
