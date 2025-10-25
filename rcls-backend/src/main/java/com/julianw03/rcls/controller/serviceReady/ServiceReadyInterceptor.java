package com.julianw03.rcls.controller.serviceReady;

import com.julianw03.rcls.service.riotclient.RiotClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ServiceReadyInterceptor implements HandlerInterceptor {
    private final RiotClientService riotClientService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!riotClientService.isConnectionEstablished()) {
            throw new ServiceNotReadyException();
        }
        return true;
    }
}
