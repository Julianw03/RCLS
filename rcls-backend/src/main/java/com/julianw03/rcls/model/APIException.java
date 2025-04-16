package com.julianw03.rcls.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.julianw03.rcls.service.riotclient.api.RiotClientError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@JsonIgnoreProperties(value = { "stackTrace", "cause", "suppressed", "localizedMessage" })
public class APIException extends RuntimeException {
    @Getter
    private final Object     details;
    @Getter
    private final HttpStatus status;

    public APIException(String details) {
        this("Internal Server Error", details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public APIException(RiotClientError error) {
        this("Riot Client returned an error", error, HttpStatus.BAD_GATEWAY);
    }

    public APIException(String message, Object details) {
        this(message, details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public APIException(Exception exception) {
        this("Internal Server Error", exception.getMessage());
    }

    public APIException(String message, Object details, HttpStatus status) {
        super(message);
        this.details = details;
        this.status = status;
    }
}
