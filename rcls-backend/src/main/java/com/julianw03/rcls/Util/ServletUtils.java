package com.julianw03.rcls.Util;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.riotclient.api.InternalApiResponse;

import java.util.Objects;

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

    public static void assertSuccessStatus(
            String scope,
            InternalApiResponse response
    ) {
        if (!response.isSuccessful()) {
            final String message = String.format("[%s]: Expected success response (2xx), but response wasn't successful",
                    scope
            );

            throw new APIException("Processing failed", message);
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
