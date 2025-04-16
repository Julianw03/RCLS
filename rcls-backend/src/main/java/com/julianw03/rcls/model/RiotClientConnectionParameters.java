package com.julianw03.rcls.model;

import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@ToString
public class RiotClientConnectionParameters {
    private final String  authSecret;
    private final Integer port;
    private final String  authHeader;

    public RiotClientConnectionParameters(
            String authSecret,
            Integer port
    ) {
        if (authSecret == null || port == null) throw new IllegalArgumentException();
        this.authSecret = authSecret;
        this.port = port;

        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(("riot:"+authSecret).getBytes(StandardCharsets.UTF_8));
    }
}
