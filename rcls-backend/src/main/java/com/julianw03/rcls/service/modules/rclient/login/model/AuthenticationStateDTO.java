package com.julianw03.rcls.service.modules.rclient.login.model;

import com.julianw03.rcls.model.services.ServiceDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.auth.AuthenticationState;
import com.julianw03.rcls.service.modules.rclient.login.model.auth.LoggedOutState;
import com.julianw03.rcls.service.modules.rclient.login.model.auth.MultifactorRequiredState;
import lombok.Builder;
import org.jetbrains.annotations.Contract;

@Builder
public record AuthenticationStateDTO(
        LoginStatusDTO loginStatus,
        HCaptchaDTO hCaptcha,
        MultifactorInfoDTO mutifactorInfo,
        String countryCode
) implements ServiceDTO<AuthenticationState> {

    @Contract(pure = true)
    public static AuthenticationStateDTO map(AuthenticationState state) {
        final AuthenticationStateDTOBuilder builder = new AuthenticationStateDTOBuilder();
        builder.loginStatus(state.getDiscriminator());
        switch (state) {
            case MultifactorRequiredState mfaState -> {
                builder.mutifactorInfo(mfaState.multifactorInfo());
            }
            case LoggedOutState loggedOutState -> {
                builder.hCaptcha(loggedOutState.hCaptcha());
            }
            default -> {

            }
        }
        return builder.build();
    }
}
