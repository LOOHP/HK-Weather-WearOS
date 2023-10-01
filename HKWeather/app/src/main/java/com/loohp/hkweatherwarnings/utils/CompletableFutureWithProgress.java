package com.loohp.hkweatherwarnings.utils;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CompletableFutureWithProgress<T> extends CompletableFuture<T> implements FutureWithProgress<T> {

    public static <T> CompletableFutureWithProgress<T> completedFuture(T value) {
        CompletableFutureWithProgress<T> future = new CompletableFutureWithProgress<>();
        future.complete(value);
        return future;
    }

    private final AtomicInteger progress;
    private final Set<ProgressListener> progressListeners;

    public CompletableFutureWithProgress() {
        this.progress = new AtomicInteger(Float.floatToIntBits(0F));
        this.progressListeners = ConcurrentHashMap.newKeySet();
    }

    public void setProgress(float progress) {
        float oldValue = Float.intBitsToFloat(this.progress.get());
        this.progress.set(Float.floatToIntBits(Math.max(0F, Math.min(progress, 1F))));
        progressListeners.forEach(l -> l.accept(oldValue, progress));
    }

    public void addProgress(float progress) {
        float oldValue = Float.intBitsToFloat(this.progress.get());
        float value = Float.intBitsToFloat(this.progress.updateAndGet(i -> Float.floatToIntBits(Math.max(0F, Math.min(Float.intBitsToFloat(i) + progress, 1F)))));
        progressListeners.forEach(l -> l.accept(oldValue, value));
    }

    @Override
    public float getProgress() {
        return Float.intBitsToFloat(progress.get());
    }

    @Override
    public CompletableFutureWithProgress<T> listen(ProgressListener listener) {
        progressListeners.add(listener);
        return this;
    }

    @Override
    public boolean complete(T value) {
        float oldValue = Float.intBitsToFloat(this.progress.get());
        progress.set(Float.floatToIntBits(1F));
        for (Iterator<ProgressListener> itr = progressListeners.iterator(); itr.hasNext();) {
            itr.next().accept(oldValue, 1F);
            itr.remove();
        }
        return super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        float oldValue = Float.intBitsToFloat(this.progress.get());
        progress.set(Float.floatToIntBits(1F));
        for (Iterator<ProgressListener> itr = progressListeners.iterator(); itr.hasNext();) {
            itr.next().accept(oldValue, 1F);
            itr.remove();
        }
        return super.completeExceptionally(ex);
    }
}
