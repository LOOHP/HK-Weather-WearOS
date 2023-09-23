package com.loohp.hkweatherwarnings.shared

import android.content.Context
import android.util.Pair
import androidx.wear.tiles.TileService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkweatherwarnings.background.PeriodicUpdateWorker
import com.loohp.hkweatherwarnings.tiles.WeatherOverviewTile
import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile
import com.loohp.hkweatherwarnings.utils.LocationUtils
import com.loohp.hkweatherwarnings.utils.LocationUtils.LocationResult
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo
import com.loohp.hkweatherwarnings.weather.LunarDate
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import java.lang.Long.min
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Shared {

    companion object {

        val HK_TIMEZONE: TimeZone = TimeZone.getTimeZone(ZoneId.of("Asia/Hong_Kong"))

        val DEFAULT_LOCATION: LocationResult = LocationResult.fromLatLng(22.3019444, 114.1741666)

        const val NEVER_REFRESH_INTERVAL = Long.MAX_VALUE - 600000L
        private const val BACKGROUND_SERVICE_REQUEST_TAG: String = "HK_WEATHER_BG_SERVICE"

        val REFRESH_INTERVAL: (Context) -> Long = { min(Registry.getInstance(it).refreshRate, NEVER_REFRESH_INTERVAL) }
        val FRESHNESS_TIME: (Context) -> Long = { REFRESH_INTERVAL.invoke(it) + 600000L }

        val currentWeatherInfo: DataState<CurrentWeatherInfo?> = DataState(null, FRESHNESS_TIME, { context, _ ->
            val locationType = Registry.getInstance(context).location
            val location = if (locationType.first == "GPS") LocationUtils.getGPSLocation(context).get() else LocationResult.ofNullable(locationType.second)
            val result = Registry.getInstance(context).getCurrentWeatherInfo(context, location).get()
            if (result == null) {
                UpdateResult(false, null)
            } else {
                UpdateResult(true, result)
            }
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java) })

        val currentWarnings: DataState<Set<WeatherWarningsType>> = DataState(emptySet(), FRESHNESS_TIME, { context, _ ->
            val result = Registry.getInstance(context).getActiveWarnings(context).get()
            if (result == null) UpdateResult(false, emptySet()) else UpdateResult(true, result)
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherWarningsTile::class.java) })

        val currentTips: DataState<List<Pair<String, Long>>> = DataState(emptyList(), FRESHNESS_TIME, { context, _ ->
            val result = Registry.getInstance(context).getWeatherTips(context).get()
            if (result == null) UpdateResult(false, emptyList()) else UpdateResult(true, result)
        }, { context, _ -> TileService.getUpdater(context).requestUpdate(WeatherTipsTile::class.java) })

        val convertedLunarDates: MapValueState<LocalDate, LunarDate> = MapValueState(ConcurrentHashMap()) { key, context, _ ->
            Registry.getInstance(context).getLunarDate(context, key).get()
        }

        fun startBackgroundService(context: Context) {
            val interval = REFRESH_INTERVAL.invoke(context)
            if (interval >= NEVER_REFRESH_INTERVAL) {
                WorkManager.getInstance(context).cancelUniqueWork(BACKGROUND_SERVICE_REQUEST_TAG)
            } else {
                val updateRequest = PeriodicWorkRequestBuilder<PeriodicUpdateWorker>(interval, TimeUnit.MILLISECONDS, PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(BACKGROUND_SERVICE_REQUEST_TAG, ExistingPeriodicWorkPolicy.UPDATE, updateRequest)
            }
        }

    }

}