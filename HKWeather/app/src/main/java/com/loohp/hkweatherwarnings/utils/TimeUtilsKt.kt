package com.loohp.hkweatherwarnings.utils

import com.loohp.hkweatherwarnings.shared.Shared
import java.time.LocalTime

fun LocalTime.nextOccurrenceIsCloserThan(other: LocalTime, referenceTime: LocalTime = LocalTime.now(Shared.HK_TIMEZONE.toZoneId())): Boolean {
    return (isBefore(other) && isAfter(referenceTime)) || (isAfter(other) && other.isBefore(referenceTime))
}