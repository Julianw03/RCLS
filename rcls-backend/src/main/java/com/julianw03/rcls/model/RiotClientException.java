package com.julianw03.rcls.model;

import com.julianw03.rcls.service.base.riotclient.api.RiotClientError;

public class RiotClientException extends RuntimeException {
    private final RiotClientError error;
    public RiotClientException(RiotClientError error) {
        super(error.getMessage());
        this.error = error;
    }
}
