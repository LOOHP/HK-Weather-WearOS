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

package com.loohp.hkweatherwarnings.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import kotlin.math.absoluteValue


inline val Dp.sp: TextUnit @Composable get() = with (LocalDensity.current) { this@sp.toSp() }

inline val TextUnit.dp: Dp @Composable get() = with (LocalDensity.current) { this@dp.toDp() }

inline val Float.equivalentDp: Dp @Composable get() = with (LocalDensity.current) { this@equivalentDp.toDp() }

fun Float.sameValueAs(other: Float) : Boolean {
    return (this - other).absoluteValue < 0.00001
}


@Composable
fun TextUnit.clamp(min: Dp? = null, max: Dp? = null) = with (LocalDensity.current) {
    var dp = dp
    if (min != null) {
        dp = dp.coerceAtLeast(min)
    }
    if (max != null) {
        dp = dp.coerceAtMost(max)
    }
    return@with dp.sp
}


fun clampSp(context: Context, sp: Float, dpMin: Float? = null, dpMax: Float? = null): Float {
    var dp = UnitUtils.spToDp(context, sp)
    if (dpMin != null) {
        dp = dp.coerceAtLeast(dpMin)
    }
    if (dpMax != null) {
        dp = dp.coerceAtMost(dpMax)
    }
    return UnitUtils.dpToSp(context, dp)
}
