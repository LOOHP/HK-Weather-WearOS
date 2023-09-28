package com.loohp.hkweatherwarnings.utils

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

fun <T> Future<T>.orElseGet(timeout: Long, unit: TimeUnit, elseGet: () -> T, printErr: Boolean = true): T {
    return try {
        get(timeout, unit)
    } catch (e: Throwable) {
        if (printErr) {
            e.printStackTrace()
        }
        elseGet.invoke()
    }
}

fun <T> Future<T>.orElse(timeout: Long, unit: TimeUnit, el: T, printErr: Boolean = true): T {
    return orElseGet(timeout, unit, { el }, printErr)
}