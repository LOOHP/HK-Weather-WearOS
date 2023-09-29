package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CompletableFutureWithProgress<T> extends CompletableFuture<T> implements FutureWithProgress<T> {

    public static <T> CompletableFutureWithProgress<T> completedFuture(T value) {
        CompletableFutureWithProgress<T> future = new CompletableFutureWithProgress<>();
        future.complete(value);
        return future;
    }

    private final AtomicInteger progress;

    public CompletableFutureWithProgress() {
        this.progress = new AtomicInteger(Float.floatToIntBits(0F));
    }

    public void setProgress(float progress) {
        this.progress.set(Float.floatToIntBits(Math.max(0F, Math.min(progress, 1F))));
    }

    public void addProgress(float progress) {
        this.progress.updateAndGet(i -> Float.floatToIntBits(Math.max(0F, Math.min(Float.intBitsToFloat(i) + progress, 1F))));
    }

    @Override
    public float getProgress() {
        return Float.intBitsToFloat(progress.get());
    }

    @Override
    public boolean complete(T value) {
        progress.set(Float.floatToIntBits(1F));
        return super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        progress.set(Float.floatToIntBits(1F));
        return super.completeExceptionally(ex);
    }
}
