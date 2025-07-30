package com.julianw03.rcls.service.rest.login;

import java.util.concurrent.ExecutionException;

public interface LoginV1Service {
    void resetHCaptcha() throws ExecutionException, IllegalStateException;
    HCaptchaDTO getHCaptcha() throws ExecutionException, IllegalStateException;

    LoginStatusDTO getLoginStatus() throws ExecutionException;
    LoginStatusDTO login(LoginInputDTO loginInput) throws ExecutionException, MultifactorRequiredException, IllegalStateException, IllegalArgumentException;
    LoginStatusDTO loginWithMultifactor(MultifactorInputDTO multifactorInputDTO) throws ExecutionException, IllegalStateException, IllegalArgumentException;
    void logout() throws ExecutionException, IllegalStateException;
}
