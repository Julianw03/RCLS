package com.julianw03.rcls.service.rest.login;

import lombok.Data;

@Data
public class LoginInputDTO {
    private String username;
    private String password;
    private String captcha;
    private Boolean remember = false;
}
