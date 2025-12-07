package com.julianw03.rcls.controller.interceptors.loggedIn;

import com.julianw03.rcls.model.data.ObjectDataManager;
import com.julianw03.rcls.service.modules.rclient.login.RsoAuthenticationManager;
import com.julianw03.rcls.service.modules.rclient.login.model.LoginStatusDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.auth.AuthenticationState;
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
                .map(AuthenticationState::getDiscriminator)
                .ifPresent(loginStatus -> {
                    if (!LoginStatusDTO.LOGGED_IN.equals(loginStatus)) {
                        throw new UserNotLoggedInException();
                    }
                });

        return true;
    }
}
