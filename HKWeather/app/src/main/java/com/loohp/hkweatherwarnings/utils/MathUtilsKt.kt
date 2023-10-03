package com.loohp.hkweatherwarnings.utils

import kotlin.math.floor
import kotlin.math.roundToInt

fun Float.floorToInt(): Int {
    return floor(this).roundToInt()
}