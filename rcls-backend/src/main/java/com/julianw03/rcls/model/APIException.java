package com.julianw03.rcls.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.julianw03.rcls.service.base.riotclient.api.RiotClientError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@JsonIgnoreProperties(value = { "stackTrace", "cause", "suppressed", "localizedMessage" })
public class APIException extends RuntimeException {
    @Getter
    private final Object     details;
    @Getter
    private final HttpStatus status;

    public APIException(String details) {
        this("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    public APIException(String details, HttpStatus status) {
        this(status.getReasonPhrase(), status, details);
    }

    public APIException(RiotClientError error) {
        this("Riot Client returned an error", HttpStatus.BAD_GATEWAY, error);
    }

    public APIException(String message, Object details) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    public APIException(Exception exception) {
        this("Internal Server Error", exception.getMessage());
    }

    public APIException(String message, HttpStatus status, Object details) {
        super(message);
        if (details instanceof Exception) {
            details = ((Exception) details).getMessage();
        }
        this.details = details;
        this.status = status;
    }
}
