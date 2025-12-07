package com.julianw03.rcls.service.modules.rclient.login.model.auth;

import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;

public record LoggedInState(

) implements AuthenticationState {
    @Override
    public LoginStatusDTO getDiscriminator() {
        return LoginStatusDTO.LOGGED_IN;
    }
}
