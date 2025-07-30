package com.julianw03.rcls.service.rest.login;

import lombok.Data;

@Data
public class MultifactorInputDTO {
    String otp;
    Boolean rememberDevice;
}
