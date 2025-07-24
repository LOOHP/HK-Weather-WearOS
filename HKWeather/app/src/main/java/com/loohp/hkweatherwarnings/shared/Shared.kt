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

package com.loohp.hkweatherwarnings.shared

import android.content.ComponentName
import android.content.Context
import android.util.Pair
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkweatherwarnings.background.PeriodicUpdateWorker
import com.loohp.hkweatherwarnings.complications.ChanceOfRainComplication
import com.loohp.hkweatherwarnings.complications.HumidityComplication
import com.loohp.hkweatherwarnings.complications.MoonriseMoonsetComplication
import com.loohp.hkweatherwarnings.complications.SunriseSunsetComplication
import com.loohp.hkweatherwarnings.complications.TemperatureRangeComplication
import com.loohp.hkweatherwarnings.complications.UVIndexComplication
import com.loohp.hkweatherwarnings.complications.WeatherAlertsComplication
import com.loohp.hkweatherwarnings.complications.WeatherTemperatureComplication
import com.loohp.hkweatherwarnings.complications.WindComplication
import com.loohp.hkweatherwarnings.tiles.WeatherOverviewTile
import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile
import com.loohp.hkweatherwarnings.utils.LocationUtils
import com.loohp.hkweatherwarnings.utils.LocationUtils.LocationResult
import com.loohp.hkweatherwarnings.utils.orElse
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo
import com.loohp.hkweatherwarnings.weather.LunarDate
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.lang.Long.min
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneId
import java.util.EnumMap
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class Shared {

    companion object {

        val HK_TIMEZONE: TimeZone = TimeZone.getTimeZone(ZoneId.of("Asia/Hong_Kong"))

        val DEFAULT_LOCATION: LocationResult = LocationResult.fromLatLng(22.3019444, 114.1741666)

        const val NEVER_REFRESH_INTERVAL = Long.MAX_VALUE - 600000L
        private const val BACKGROUND_SERVICE_REQUEST_TAG: String = "HK_WEATHER_BG_SERVICE"

        val REFRESH_INTERVAL: (Context) -> Long = { min(Registry.getInstance(it).refreshRate, NEVER_REFRESH_INTERVAL) }
        val FRESHNESS_TIME: (Context) -> Long = { REFRESH_INTERVAL.invoke(it) + 600000L }

        private const val WEATHER_CACHE_FILE = "weather_cache.json"
        private const val WARNINGS_CACHE_FILE = "warnings_cache.json"
        private const val TIPS_CACHE_FILE = "tips_cache.json"

        val currentWeatherInfo: DataState<CurrentWeatherInfo?> = DataState(null, {
            if (it.applicationContext.fileList().contains(WEATHER_CACHE_FILE)) {
                try {
                    BufferedReader(InputStreamReader(it.applicationContext.openFileInput(WEATHER_CACHE_FILE), StandardCharsets.UTF_8)).use { reader ->
                        val json = JSONObject(reader.lines().collect(Collectors.joining()))
                        val data = if (json.has("weather")) CurrentWeatherInfo.deserialize(json.optJSONObject("weather")!!) else null
                        val updateTime = json.optLong("updateTime")
                        val updateSuccessful = json.optBoolean("updateSuccessful")
                        return@DataState DataStateInitializeResult(data, updateTime, updateSuccessful, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    it.applicationContext.deleteFile(WEATHER_CACHE_FILE)
                }
            }
            DataStateInitializeResult.defaultEmpty(null)
        }, {
            it.applicationContext.deleteFile(WEATHER_CACHE_FILE)
        }, FRESHNESS_TIME, { context, _, updateProgress ->
            val locationType = Registry.getInstance(context).location
            val location = if (locationType.first == "GPS") LocationUtils.getGPSLocation(context).get() else LocationResult.ofNullable(locationType.second)
            val result = Registry.getInstance(context).getCurrentWeatherInfo(context, location).listen { _, value -> updateProgress.value = value }.orElse(60, TimeUnit.SECONDS, null)
            if (result == null) UpdateResult.failed() else UpdateResult.success(result)
        }, { context, self, value ->
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, WeatherTemperatureComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, TemperatureRangeComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, HumidityComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, UVIndexComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, ChanceOfRainComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, SunriseSunsetComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, MoonriseMoonsetComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, WindComplication::class.java)).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, WeatherAlertsComplication::class.java)).requestUpdateAll()
            try {
                PrintWriter(OutputStreamWriter(context.applicationContext.openFileOutput(WEATHER_CACHE_FILE, Context.MODE_PRIVATE), StandardCharsets.UTF_8)).use {
                    val json = JSONObject()
                    value?.let { v -> json.put("weather", v.serialize()) }
                    json.put("updateTime", self.getLastSuccessfulUpdateTime(context))
                    json.put("updateSuccessful", self.isLastUpdateSuccess(context))
                    it.write(json.toString())
                    it.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { context, _ ->
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
        })

        val currentWarnings: DataState<Map<WeatherWarningsType, String?>> = DataState(emptyMap(), {
            if (it.applicationContext.fileList().contains(WARNINGS_CACHE_FILE)) {
                try {
                    BufferedReader(InputStreamReader(it.applicationContext.openFileInput(WARNINGS_CACHE_FILE), StandardCharsets.UTF_8)).use { reader ->
                        val json = JSONObject(reader.lines().collect(Collectors.joining()))
                        val entries = json.optJSONArray("warnings")!!
                        val map: MutableMap<WeatherWarningsType, String?> = EnumMap(WeatherWarningsType::class.java)
                        for (i in 0 until entries.length()) {
                            val entry = entries.optJSONObject(i)!!
                            val type = WeatherWarningsType.valueOf(entry.optString("type").uppercase())
                            val text = entry.optString("text").ifEmpty { null }
                            map[type] = text
                        }
                        val updateTime = json.optLong("updateTime")
                        val updateSuccessful = json.optBoolean("updateSuccessful")
                        return@DataState DataStateInitializeResult(map, updateTime, updateSuccessful, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    it.applicationContext.deleteFile(WARNINGS_CACHE_FILE)
                }
            }
            DataStateInitializeResult.defaultEmpty(emptyMap())
        }, {
            it.applicationContext.deleteFile(WARNINGS_CACHE_FILE)
        }, FRESHNESS_TIME, { context, _, _ ->
            val result = Registry.getInstance(context).getActiveWarnings(context).orElse(60, TimeUnit.SECONDS, null)
            if (result == null) UpdateResult.failed() else UpdateResult.success(result)
        }, { context, self, value ->
            TileService.getUpdater(context).requestUpdate(WeatherWarningsTile::class.java)
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, WeatherAlertsComplication::class.java)).requestUpdateAll()
            PrintWriter(OutputStreamWriter(context.applicationContext.openFileOutput(WARNINGS_CACHE_FILE, Context.MODE_PRIVATE), StandardCharsets.UTF_8)).use {
                try {
                    val array = JSONArray()
                    for ((type, text) in value.entries) {
                        val obj = JSONObject()
                        obj.put("type", type.name)
                        obj.put("text", text?: "")
                        array.put(obj)
                    }
                    val json = JSONObject()
                    json.put("warnings", array)
                    json.put("updateTime", self.getLastSuccessfulUpdateTime(context))
                    json.put("updateSuccessful", self.isLastUpdateSuccess(context))
                    it.write(json.toString())
                    it.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, { context, _ ->
            TileService.getUpdater(context).requestUpdate(WeatherWarningsTile::class.java)
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
        })

        val currentTips: DataState<List<Pair<String, Long>>> = DataState(emptyList(), {
            if (it.applicationContext.fileList().contains(TIPS_CACHE_FILE)) {
                try {
                    BufferedReader(InputStreamReader(it.applicationContext.openFileInput(TIPS_CACHE_FILE), StandardCharsets.UTF_8)).use { reader ->
                        val json = JSONObject(reader.lines().collect(Collectors.joining()))
                        val entries = json.optJSONArray("tips")!!
                        val list = buildList<Pair<String, Long>> {
                            for (i in 0 until entries.length()) {
                                val entry = entries.optJSONObject(i)!!
                                val tip = entry.optString("tip")
                                val time = entry.optLong("time")
                                add(Pair.create(tip, time))
                            }
                        }
                        val updateTime = json.optLong("updateTime")
                        val updateSuccessful = json.optBoolean("updateSuccessful")
                        return@DataState DataStateInitializeResult(list, updateTime, updateSuccessful, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    it.applicationContext.deleteFile(TIPS_CACHE_FILE)
                }
            }
            DataStateInitializeResult.defaultEmpty(emptyList())
        }, {
            it.applicationContext.deleteFile(TIPS_CACHE_FILE)
        }, FRESHNESS_TIME, { context, _, _ ->
            val result = Registry.getInstance(context).getWeatherTips(context).orElse(60, TimeUnit.SECONDS, null)
            if (result == null) UpdateResult.failed() else UpdateResult.success(result)
        }, { context, self, value ->
            TileService.getUpdater(context).requestUpdate(WeatherTipsTile::class.java)
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, WeatherAlertsComplication::class.java)).requestUpdateAll()
            PrintWriter(OutputStreamWriter(context.applicationContext.openFileOutput(TIPS_CACHE_FILE, Context.MODE_PRIVATE), StandardCharsets.UTF_8)).use {
                try {
                    val array = JSONArray()
                    for (pair in value) {
                        val obj = JSONObject()
                        obj.put("tip", pair.first)
                        obj.put("time", pair.second)
                        array.put(obj)
                    }
                    val json = JSONObject()
                    json.put("tips", array)
                    json.put("updateTime", self.getLastSuccessfulUpdateTime(context))
                    json.put("updateSuccessful", self.isLastUpdateSuccess(context))
                    it.write(json.toString())
                    it.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, { context, _ ->
            TileService.getUpdater(context).requestUpdate(WeatherTipsTile::class.java)
            TileService.getUpdater(context).requestUpdate(WeatherOverviewTile::class.java)
        })

        val convertedLunarDates: MapValueState<LocalDate, LunarDate> = MapValueState(ConcurrentHashMap(), ConcurrentHashMap()) { key, context, _ ->
            val result = Registry.getInstance(context).getLunarDate(context, key).orElse(60, TimeUnit.SECONDS, null)
            if (result == null) UpdateResult.failed() else UpdateResult.success(result)
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