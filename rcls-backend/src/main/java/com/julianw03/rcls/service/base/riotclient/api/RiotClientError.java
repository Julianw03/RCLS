package com.julianw03.rcls.service.base.riotclient.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
public class RiotClientError {
    private String   errorCode;
    private Integer  httpStatus;
    private JsonNode implementationDetails;
    private String   message;
}
