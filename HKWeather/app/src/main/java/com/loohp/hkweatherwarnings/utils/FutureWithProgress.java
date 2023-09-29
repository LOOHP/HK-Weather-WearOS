package com.loohp.hkweatherwarnings.utils;

import java.util.concurrent.Future;

public interface FutureWithProgress<T> extends Future<T> {

    float getProgress();

}
