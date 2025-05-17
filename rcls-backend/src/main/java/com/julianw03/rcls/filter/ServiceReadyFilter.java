package com.julianw03.rcls.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Slf4j
public class ServiceReadyFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectNode errorNode;


    private final RiotClientService riotClientService;


    public ServiceReadyFilter(@Autowired RiotClientService riotClientService) {
        this.riotClientService = riotClientService;
        errorNode = objectMapper.createObjectNode();
        errorNode.put("error", "Service is not ready");
        errorNode.put("message", "Please connect to the Riot Client first");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!riotClientService.isConnectionEstablished()) {
            httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            httpResponse.setHeader(HttpHeaders.CONTENT_TYPE, "application/json" );
            httpResponse.getWriter().write(errorNode.toString());
            return;
        }

        chain.doFilter(request, response);
    }
}
