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

import kotlin.math.roundToInt

private val directionsEn = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private val directionsZh = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")


fun Float.toCardinalDirectionString(language: String): String {
    var angle = this % 360F
    if (angle < 0) {
        angle = 360F - angle
    }
    return (if (language == "en") directionsEn else directionsZh)[(angle / 45F).roundToInt() % 8]
}
