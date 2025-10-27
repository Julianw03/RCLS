package com.julianw03.rcls.controller.interceptors.serviceReady;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ServiceNotReadyException extends ErrorResponseException {
    public ServiceNotReadyException() {
        super(HttpStatus.SERVICE_UNAVAILABLE);
        setTitle("Service is not ready");
        setDetail("Please connect to the Riot Client first");
    }
}
