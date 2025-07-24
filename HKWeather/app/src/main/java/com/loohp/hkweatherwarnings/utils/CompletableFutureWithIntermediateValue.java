/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
