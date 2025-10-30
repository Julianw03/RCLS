package com.julianw03.rcls.Util;

import org.springframework.util.function.ThrowingFunction;
import org.springframework.util.function.ThrowingSupplier;

import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FutureUtils {
    private FutureUtils() {
    }

    public static <T> Supplier<T> wrapIntoExceptionSupplier(ThrowingSupplier<T> supplier) throws CompletionException {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                return sneakyThrow(t);
            }
        };
    }

    public static <I, R> Function<I, R> wrapIntoExceptionFunction(ThrowingFunction<I, R> func) throws CompletionException {
        return (i) -> {
            try {
                return func.apply(i);
            } catch (Throwable t) {
                return sneakyThrow(t);
            }
        };
    }

    private static <E extends Throwable, R> R sneakyThrow(Throwable t) throws CompletionException {
        switch (t) {
            case RuntimeException re -> throw re;
            case Error err -> throw err;
            default -> throw new CompletionException(t);
        }
    }
}
