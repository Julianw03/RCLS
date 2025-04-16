package com.julianw03.rcls.components;

import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RiotClientServiceHealthIndicator implements HealthIndicator {

    private final RiotClientService service;

    @Autowired
    public RiotClientServiceHealthIndicator(RiotClientService service) {
        this.service = service;
    }

    @Override
    public Health health() {
        if (!service.isConnectionEstablished()) {
            return Health
                    .down()
                    .withDetail("connected",false)
                    .build();
        }
        return Health
                .up()
                .withDetail("connected", true)
                .build();
    }
}
