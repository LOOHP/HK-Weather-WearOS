package com.loohp.hkweatherwarnings.utils

import java.text.DateFormat
import java.util.TimeZone


fun DateFormat.timeZone(timeZone: TimeZone): DateFormat {
    this.timeZone = timeZone
    return this
}