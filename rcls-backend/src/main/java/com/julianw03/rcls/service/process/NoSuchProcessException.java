package com.julianw03.rcls.service.process;

public class NoSuchProcessException extends RuntimeException {
    public NoSuchProcessException(String message) {
        super(message);
    }
}
