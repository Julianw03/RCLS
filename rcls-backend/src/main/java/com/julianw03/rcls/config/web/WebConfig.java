package com.julianw03.rcls.config.web;

import com.julianw03.rcls.controller.interceptors.loggedIn.UserLoggedInInterceptor;
import com.julianw03.rcls.controller.interceptors.serviceReady.ServiceReadyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ServiceReadyInterceptor serviceReadyInterceptor;
    private final UserLoggedInInterceptor userLoggedInInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceReadyInterceptor)
                .addPathPatterns("/api/riotclient/**");

        registry.addInterceptor(userLoggedInInterceptor)
                .addPathPatterns("/api/riotclient/launcher/v1/**");
    }
}
