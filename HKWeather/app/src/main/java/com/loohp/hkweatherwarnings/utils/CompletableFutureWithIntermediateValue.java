package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureWithIntermediateValue<T> extends CompletableFuture<T> implements FutureWithIntermediateValue<T> {

    public static <T> CompletableFutureWithIntermediateValue<T> completedFuture(T value) {
        CompletableFutureWithIntermediateValue<T> future = new CompletableFutureWithIntermediateValue<>(null);
        future.complete(value);
        return future;
    }

    private final T intermediateValue;

    public CompletableFutureWithIntermediateValue(T intermediateValue) {
        this.intermediateValue = intermediateValue;
    }

    @Override
    public T getIntermediateValue() {
        return intermediateValue;
    }

    @Override
    public T getOrIntermediateValueNow() {
        return getNow(getIntermediateValue());
    }

    @Override
    public T getOrIntermediateValue() throws ExecutionException, InterruptedException {
        if (isDone()) {
            return get();
        }
        if (hasIntermediateValue()) {
            return getIntermediateValue();
        }
        return get();
    }

    @Override
    public boolean hasIntermediateValue() {
        return intermediateValue != null;
    }

}
