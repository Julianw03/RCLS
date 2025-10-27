package com.julianw03.rcls.controller.interceptors.loggedIn;

import com.julianw03.rcls.model.data.ObjectDataManager;
import com.julianw03.rcls.service.modules.rclient.login.RsoAuthenticationManager;
import com.julianw03.rcls.service.modules.rclient.login.model.AuthenticationStateDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLoggedInInterceptor implements HandlerInterceptor {
    private final RsoAuthenticationManager rsoAuthenticationManager;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws UserNotLoggedInException {
        Optional.ofNullable(rsoAuthenticationManager)
                .map(ObjectDataManager::getView)
                .map(AuthenticationStateDTO::getLoginStatus)
                .ifPresent(loginStatus -> {
                    if (!LoginStatusDTO.LOGGED_IN.equals(loginStatus)) {
                        throw new UserNotLoggedInException();
                    }
                });

        return true;
    }
}
