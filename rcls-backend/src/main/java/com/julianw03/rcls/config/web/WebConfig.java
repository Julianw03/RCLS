package com.julianw03.rcls.config.web;

import com.julianw03.rcls.controller.serviceReady.ServiceReadyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ServiceReadyInterceptor serviceReadyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceReadyInterceptor)
                .addPathPatterns("/api/riotclient/**");
    }
}
