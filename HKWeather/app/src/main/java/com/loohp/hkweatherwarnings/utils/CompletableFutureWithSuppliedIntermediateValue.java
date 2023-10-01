package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class CompletableFutureWithSuppliedIntermediateValue<T> extends CompletableFuture<T> implements FutureWithIntermediateValue<T> {

    public static <T> CompletableFutureWithSuppliedIntermediateValue<T> completedFuture(T value) {
        CompletableFutureWithSuppliedIntermediateValue<T> future = new CompletableFutureWithSuppliedIntermediateValue<>(null);
        future.complete(value);
        return future;
    }

    private final Supplier<T> intermediateValueSupplier;

    public CompletableFutureWithSuppliedIntermediateValue(Supplier<T> intermediateValueSupplier) {
        this.intermediateValueSupplier = intermediateValueSupplier;
    }

    @Override
    public T getIntermediateValue() {
        return intermediateValueSupplier == null ? null : intermediateValueSupplier.get();
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
        return intermediateValueSupplier != null && intermediateValueSupplier.get() != null;
    }

}
