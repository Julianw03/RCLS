package com.julianw03.rcls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"com.julianw03.rcls"}, exclude = {org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
public class RCLSApplication {
    public static void main(String[] args) {
        SpringApplication.run(
                RCLSApplication.class,
                args
        );
    }
}
