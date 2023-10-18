/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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