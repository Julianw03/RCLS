package com.julianw03.rcls.service.modules.rclient.login.model.auth;

import com.julianw03.rcls.service.modules.rclient.login.model.HCaptchaDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;

public record LoggedOutState(
        HCaptchaDTO hCaptcha
) implements AuthenticationState {
    @Override
    public LoginStatusDTO getDiscriminator() {
        return LoginStatusDTO.LOGGED_OUT;
    }
}
