package com.loohp.hkweatherwarnings.shared

import android.util.Pair
import com.loohp.hkweatherwarnings.utils.LocationUtils.LocationResult
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo
import com.loohp.hkweatherwarnings.weather.LunarDate
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

class Shared {

    companion object {

        val HK_TIMEZONE: TimeZone = TimeZone.getTimeZone(ZoneId.of("Asia/Hong_Kong"))

        val DEFAULT_LOCATION: LocationResult = LocationResult.fromLatLng(22.3019444, 114.1741666)

        const val DEFAULT_REFRESH_INTERVAL: Long = 900000

        var currentWeatherInfoLastUpdated: Long = 0
        var currentWeatherInfo: CurrentWeatherInfo? = null

        var currentWarningsLastUpdated: Long = 0
        var currentWarnings: Set<WeatherWarningsType> = emptySet()

        var currentTipsLastUpdated: Long = 0
        var currentTips: List<Pair<String, Long>> = emptyList()

        val convertedLunarDates: MutableMap<LocalDate, LunarDate> = ConcurrentHashMap()

    }

}