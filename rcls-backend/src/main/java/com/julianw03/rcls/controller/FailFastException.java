package com.julianw03.rcls.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class FailFastException extends ErrorResponseException {
    /**
     * This exception should be thrown to indicate that a certain operation has not finished its execution
     * in a timely manner and that the operation has therefore been aborted by the underlying system.
     * */
    public FailFastException() {
        super(HttpStatus.GATEWAY_TIMEOUT);
    }

    public FailFastException(
            ProblemDetail body,
            Throwable cause
    ) {
        super(
                HttpStatus.GATEWAY_TIMEOUT,
                body,
                cause
        );
    }
}
