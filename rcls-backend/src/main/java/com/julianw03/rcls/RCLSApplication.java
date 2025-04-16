package com.julianw03.rcls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication(scanBasePackages = { "com.julianw03.rcls" })
public class RCLSApplication {
    private static final Logger log = LoggerFactory.getLogger(RCLSApplication.class);

    @Value("${spring.application.developer.enabled}")
    private String developerModeEnabled;

    public static void main(String[] args) {
        SpringApplication.run(RCLSApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        if (String.valueOf(true).equals(developerModeEnabled)) {
            log.error("RUNNING DEVELOPER MODE !!!");
        }
    }
}
