package com.julianw03.rcls.service.modules.rclient.patchHandler.model;

public class PatchingNotAvailableException extends RuntimeException {
    public PatchingNotAvailableException(String message) {
        super(message);
    }
}
