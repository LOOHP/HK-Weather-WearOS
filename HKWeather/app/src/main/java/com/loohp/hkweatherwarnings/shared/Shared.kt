package com.loohp.hkweatherwarnings.shared

import android.util.Pair
import androidx.wear.tiles.TileService
import com.loohp.hkweatherwarnings.tiles.WeatherOverviewTile
import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile
import com.loohp.hkweatherwarnings.utils.LocationUtils
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

        const val FRESHNESS_TIME: Long = 2400000
        const val REFRESH_INTERVAL: Long = 1800000
        const val BACKGROUND_SERVICE_REQUEST_TAG: String = "HK_WEATHER_BG_SERVICE"

        val currentWeatherInfo: DataState<CurrentWeatherInfo?> = DataState(null, { FRESHNESS_TIME }, { context, _ ->
            val locationType = Registry.getInstance(context).location
            val location = if (locationType.first == "GPS") LocationUtils.getGPSLocation(context).get() else LocationResult.ofNullable(locationType.second)
            val result = Registry.getInstance(context).getCurrentWeatherInfo(context, location).get()
            if (result == null) {
                UpdateResult(false, null)
            } else {
                UpdateResult(true, result)
            }
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java) })

        val currentWarnings: DataState<Set<WeatherWarningsType>> = DataState(emptySet(), { FRESHNESS_TIME }, { context, _ ->
            val result = Registry.getInstance(context).getActiveWarnings(context).get()
            if (result == null) UpdateResult(false, emptySet()) else UpdateResult(true, result)
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherWarningsTile::class.java) })

        val currentTips: DataState<List<Pair<String, Long>>> = DataState(emptyList(), { FRESHNESS_TIME }, { context, _ ->
            val result = Registry.getInstance(context).getWeatherTips(context).get()
            if (result == null) UpdateResult(false, emptyList()) else UpdateResult(true, result)
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherTipsTile::class.java) })

        val convertedLunarDates: MapValueState<LocalDate, LunarDate> = MapValueState(ConcurrentHashMap()) { key, context, _ ->
            Registry.getInstance(context).getLunarDate(context, key).get()
        }

    }

}