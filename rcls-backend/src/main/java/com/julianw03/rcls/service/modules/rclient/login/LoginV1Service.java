package com.julianw03.rcls.service.modules.rclient.login;

import com.julianw03.rcls.controller.InvalidCredentialsException;
import com.julianw03.rcls.service.modules.rclient.login.model.*;

import java.util.concurrent.ExecutionException;

public interface LoginV1Service {
    void resetHCaptcha() throws ExecutionException, IllegalStateException;

    HCaptchaDTO getHCaptcha() throws ExecutionException, IllegalStateException;

    LoginStatusDTO getLoginStatus() throws ExecutionException;

    LoginStatusDTO login(LoginInputDTO loginInput) throws ExecutionException, MultifactorRequiredException, IllegalStateException, InvalidCredentialsException;

    LoginStatusDTO loginWithMultifactor(MultifactorInputDTO multifactorInputDTO) throws ExecutionException, IllegalStateException, InvalidCredentialsException;

    void logout() throws ExecutionException, IllegalStateException;
}
