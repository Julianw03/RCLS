package com.julianw03.rcls.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.julianw03.rcls.service.base.riotclient.api.RiotClientError;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Objects;

@JsonIgnoreProperties(value = {"stackTrace", "cause", "suppressed", "localizedMessage"})
public class APIException extends RuntimeException {
    @Getter
    @Schema(description = "Details about the error that occurred")
    private final String     details;
    @Getter
    @Schema(description = "HTTP status code associated with the error")
    private final HttpStatus status;

    public APIException(String details) {
        this("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    public APIException(String details, HttpStatus status) {
        this(status.getReasonPhrase(), status, details);
    }

    public APIException(String details, Exception e) {
        this("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    public APIException(RiotClientError error) {
        this("Riot Client returned an error", HttpStatus.BAD_GATEWAY, error.getMessage());
    }

    public APIException(String message, HttpStatus status, String details) {
        super(message);
        this.details = details;
        this.status = status;
    }

    public APIException(Exception exception) {
        this("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    public static Builder builder(String message) {
        return new Builder(message);
    }

    public static Builder builder(HttpStatus status) {
        return new Builder(status.getReasonPhrase()).status(status);
    }

    public static class Builder {
        private final String message;
        private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        private String details = "";

        public Builder(String message) {
            this.message = Objects.requireNonNull(message, "Message cannot be null");
        }

        public Builder status(HttpStatus status) {
            this.status = Objects.requireNonNull(status, "Status cannot be null");
            return this;
        }

        public Builder details(String details) {
            this.details = Objects.requireNonNull(details, "Details cannot be null");
            return this;
        }

        public APIException build() {
            return new APIException(message, status, details);
        }
    }
}
