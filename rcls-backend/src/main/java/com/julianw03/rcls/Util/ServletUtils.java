package com.julianw03.rcls.Util;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.base.riotclient.api.InternalApiResponse;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServletUtils {
    public static <T> void assertEqual(
            String scope,
            T expected,
            T actual
    ) throws APIException {
        if (!Objects.equals(expected, actual)) {
            final String message = String.format("[%s]: Expected value \"%s\", but got \"%s\"",
                    scope,
                    expected,
                    actual
            );


            throw new APIException("Processing failed", message);
        }
    }


    @SafeVarargs
    public static <T> void assertFieldsNotNull(
            T obj,
            Function<T, ?>... fieldExtractor
    ) {
        if (obj == null || fieldExtractor == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }

        for (Function<T, ?> extractor : fieldExtractor) {
            if (extractor.apply(obj) == null) {
                throw new IllegalArgumentException("[AssertFieldsNotNull]: Field cannot be null");
            }
        }
    }

    public static void expectInternalApiSuccess(
            InternalApiResponse response,
            Consumer<InternalApiResponse.Success> onSuccess
    ) throws APIException {
        if (response == null || onSuccess == null) {
            throw new IllegalArgumentException("Response and onSuccess cannot be null");
        }

        switch (response) {
            case InternalApiResponse.ApiError error -> {
                throw new APIException(error.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new APIException(exception.getException());
            }
            case InternalApiResponse.Success success -> {
                onSuccess.accept(success);
            }
            default -> {
                throw new APIException("Unexpected Error Occurred");
            }
        }
    }

    public static <T> T expectInternalApiSuccess(
            InternalApiResponse response,
            Function<InternalApiResponse.Success, T> onSuccess
    ) throws APIException {
        if (response == null || onSuccess == null) {
            throw new IllegalArgumentException("Response and onSuccess cannot be null");
        }

        switch (response) {
            case InternalApiResponse.ApiError error -> {
                throw new APIException(error.getError());
            }
            case InternalApiResponse.InternalException exception -> {
                throw new APIException(exception.getException());
            }
            case InternalApiResponse.Success success -> {
                return onSuccess.apply(success);
            }
            default -> {
                throw new APIException("Unexpected Error Occurred");
            }
        }
    }

    public static void assertSuccessStatus(
            String scope,
            InternalApiResponse response
    ) {
        if (!response.isSuccessful()) {
            final String message = String.format("[%s]: Expected success response (2xx), but response wasn't successful",
                    scope
            );

            throw new IllegalStateException(message);
        }
    }

    public static void assertSuccessStatus(
            String scope,
            Integer statusCode
    ) throws APIException {
        if (!isSuccessResponseCode(statusCode)) {
            final String message = String.format("[%s]: Expected success response (2xx) but got status code %d",
                    scope,
                    statusCode
            );

            throw new APIException("Processing failed", message);
        }
    }

    public static boolean isSuccessResponseCode(Integer statusCode) {
        if (statusCode == null) return false;
        return (Math.floorDiv(statusCode, 100) == 2);
    }
}
