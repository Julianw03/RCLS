package com.julianw03.rcls.filter;

import com.julianw03.rcls.service.riotclient.RiotClientService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Slf4j
public class ServiceReadyFilter implements Filter {
    private final RiotClientService riotClientService;


    public ServiceReadyFilter(@Autowired RiotClientService riotClientService) {
        this.riotClientService = riotClientService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!riotClientService.isConnectionEstablished()) {
            httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }

        chain.doFilter(request, response);
    }
}
