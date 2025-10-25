package com.julianw03.rcls.controller;

import com.julianw03.rcls.interceptors.serviceReady.ServiceNotReadyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleServiceNotReadyException(ServiceNotReadyException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                             .body(ex.getBody());
    }
}
