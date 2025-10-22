package com.julianw03.rcls.service;

public class FailFastException extends Exception {
    /**
     * This exception should be thrown to indicate that a certain operation has not finished its execution
     * in a timely manner and that the operation has therefore been aborted by the underlying system.
     * */
    public FailFastException(String message) {
        super(message);
    }

    public FailFastException(String message, Throwable cause) {
        super(message, cause);
    }
}
