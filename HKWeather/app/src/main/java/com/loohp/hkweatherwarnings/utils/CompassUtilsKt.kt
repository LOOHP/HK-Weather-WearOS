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
