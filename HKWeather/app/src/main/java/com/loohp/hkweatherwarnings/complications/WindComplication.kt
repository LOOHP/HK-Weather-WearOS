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

package com.loohp.hkweatherwarnings.complications

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class WindComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                val dir = if (Registry.getInstance(this).language == "en") "E" else "東"
                val windText = if (Registry.getInstance(this).language == "en") "5/10" else "5/10"
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(dir).build(),
                    contentDescription = PlainComplicationText.Builder(dir).build(),
                )
                    .setTitle(PlainComplicationText.Builder(windText).build())
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                val text = if (Registry.getInstance(this).language == "en") {
                    "E 5 Gust 10 km/h"
                } else {
                    "東 5 陣風10 公里/小時"
                }
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder(text).build(),
                )
                    .setTapAction(null)
                    .build()
            }
            else -> null
        }
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        ForkJoinPool.commonPool().execute {
            val weatherInfo = Shared.currentWeatherInfo.getLatestValue(this, ForkJoinPool.commonPool()).orIntermediateValue
            if (weatherInfo == null) {
                listener.onComplicationData(null)
                return@execute
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.WIND.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        val (dir, windText) = if (weatherInfo.windSpeed < 0F) {
                            "-" to "-"
                        } else {
                            weatherInfo.windDirection to String.format("%.0f", weatherInfo.windSpeed).plus("/").plus(String.format("%.0f", weatherInfo.gust))
                        }
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(dir).build(),
                            contentDescription = PlainComplicationText.Builder(dir).build(),
                        )
                            .setTitle(PlainComplicationText.Builder(windText).build())
                            .setTapAction(pendingIntent)
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        val unit = if (Registry.getInstance(this).language == "en") "km/h" else "公里/小時"
                        val text = if (weatherInfo.windSpeed < 0F) {
                            "-"
                        } else if (Registry.getInstance(this).language == "en") {
                            weatherInfo.windDirection.plus(" ").plus(String.format("%.0f", weatherInfo.windSpeed))
                                .plus(" Gust ").plus(String.format("%.0f", weatherInfo.gust)).plus(" ").plus(unit)
                        } else {
                            weatherInfo.windDirection.plus(" ").plus(String.format("%.0f", weatherInfo.windSpeed))
                                .plus(" 陣風").plus(String.format("%.0f", weatherInfo.gust)).plus(" ").plus(unit)
                        }
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(text).build(),
                            contentDescription = PlainComplicationText.Builder(text).build(),
                        )
                            .setTapAction(pendingIntent)
                            .build()
                    }
                    else -> null
                }
            )
        }
    }

}