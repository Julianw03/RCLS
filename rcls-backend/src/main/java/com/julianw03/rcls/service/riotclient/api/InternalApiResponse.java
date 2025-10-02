package com.julianw03.rcls.service.riotclient.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

public sealed class InternalApiResponse permits InternalApiResponse.ApiError, InternalApiResponse.InternalException, InternalApiResponse.NoContent, InternalApiResponse.Success {
    @Getter
    public static final class Success extends InternalApiResponse {
        private final JsonNode data;
        public Success(JsonNode data) {
            this.data = data;
        }

        public <T> Optional<T> map(ObjectMapper mapper, Class<T> tClass) {
            try {
                return Optional.ofNullable(mapper.treeToValue(data, tClass));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        public <T> Optional<T> map(ObjectMapper mapper, TypeReference<T> tClass) {
            try {
                return Optional.ofNullable(mapper.treeToValue(data, tClass));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    @NoArgsConstructor
    public static final class NoContent extends InternalApiResponse {
    }

    @Getter
    public static final class InternalException extends InternalApiResponse {
        private final Exception exception;
        public InternalException(Exception exception) {
            this.exception = exception;
        }
    }

    @Getter
    public static final class ApiError extends InternalApiResponse {
        private final RiotClientError error;
        public ApiError(RiotClientError error) {
            this.error = error;
        }
    }

    public boolean isSuccessful() {
        switch (this) {
            case NoContent noContent -> {
                return true;
            }
            case Success success -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
