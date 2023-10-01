package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface FutureWithIntermediateValue<T> extends Future<T> {

    T getIntermediateValue();

    T getOrIntermediateValueNow();

    T getOrIntermediateValue() throws ExecutionException, InterruptedException;

    boolean hasIntermediateValue();

}
