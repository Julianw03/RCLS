package com.julianw03.rcls.service.modules.rclient.login.model.auth;

import com.julianw03.rcls.model.services.DiscriminatedSerializable;
import com.julianw03.rcls.service.modules.rclient.login.model.AuthenticationStateDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;

public sealed interface AuthenticationState extends DiscriminatedSerializable<LoginStatusDTO> permits ErrorState, LoggedInState, LoggedOutState, MultifactorRequiredState, UnknownState {
    default AuthenticationStateDTO.AuthenticationStateDTOBuilder baseBuilder() {
        return AuthenticationStateDTO.builder()
                                     .loginStatus(this.getDiscriminator());
    }
}
