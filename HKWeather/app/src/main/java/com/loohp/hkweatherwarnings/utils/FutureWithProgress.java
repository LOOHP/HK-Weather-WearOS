package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.Future;

public interface FutureWithProgress<T> extends Future<T> {

    float getProgress();

    FutureWithProgress<T> listen(ProgressListener listener);

    @FunctionalInterface
    interface ProgressListener {

        void accept(float oldValue, float newValue);

    }

}
