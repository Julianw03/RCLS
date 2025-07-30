package com.julianw03.rcls.service.rest.connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiotClientConnectionParametersDTO {
    private String  authSecret;
    private Integer port;

    private String authHeader;
}
