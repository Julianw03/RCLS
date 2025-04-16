package com.julianw03.rcls.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


@Slf4j
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final List<String> allowedMethods = Arrays.stream(HttpMethod.values()).map(HttpMethod::name).toList();

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.application.developer.enabled}") String developerFlag,
            @Value("${server.port}") String port
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(developerFlag, port)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers ->
                        headers
                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .sessionManagement(session ->
                        session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }


    public CorsConfigurationSource corsConfigurationSource(
            @Value("${spring.application.developer.enabled}") String developerFlag,
            @Value("${server.port}") String port
    ) {
        final String pattern;
        if (String.valueOf(true).equals(developerFlag)) {
            pattern = "*";
        } else {
            //Default https port -> browsers dont explicitly set port
            if ("443".equals(port)) {
                pattern = "https://127.0.0.1";
            } else {
                pattern = "https://127.0.0.1:" + port;
            }
        }

        log.warn("Using pattern: {}", pattern);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList(pattern));
        configuration.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "responseType", "Authorization"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS", "DELETE"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
