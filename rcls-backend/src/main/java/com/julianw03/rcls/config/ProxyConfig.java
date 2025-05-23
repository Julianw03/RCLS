package com.julianw03.rcls.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProxyConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
