package com.julianw03.rcls.service.modules.rclient.login.model.auth;

import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.MultifactorInfoDTO;

public record MultifactorRequiredState(
        MultifactorInfoDTO multifactorInfo
) implements AuthenticationState {
    @Override
    public LoginStatusDTO getDiscriminator() {
        return LoginStatusDTO.MULTIFACTOR_REQUIRED;
    }
}
