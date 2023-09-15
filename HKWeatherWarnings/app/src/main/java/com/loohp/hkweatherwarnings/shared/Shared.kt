package com.loohp.hkweatherwarnings.shared

import java.time.ZoneId
import java.util.TimeZone

class Shared {

    companion object {

        val HK_TIMEZONE: TimeZone = TimeZone.getTimeZone(ZoneId.of("Asia/Hong_Kong"))

    }

}