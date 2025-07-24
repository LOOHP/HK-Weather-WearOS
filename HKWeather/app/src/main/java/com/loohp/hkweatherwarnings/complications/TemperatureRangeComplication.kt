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

package com.loohp.hkweatherwarnings.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class TemperatureRangeComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val lowestTemperature = 23F
        val highestTemperature = 27F
        val icon = Icon.createWithResource(this, R.mipmap.pic51)
        return when (type) {
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.1f", lowestTemperature).plus(" - ").plus(String.format("%.1f", highestTemperature)).plus("°C")).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.1f", lowestTemperature).plus(" - ").plus(String.format("%.1f", highestTemperature)).plus("°C")).build(),
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
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
            val lowestTemperature = weatherInfo.lowestTemperature
            val highestTemperature = weatherInfo.highestTemperature
            val icon = Icon.createWithResource(this, weatherInfo.weatherIcon.iconId)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.MAIN.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.1f", lowestTemperature).plus(" - ").plus(String.format("%.1f", highestTemperature)).plus("°C")).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.1f", lowestTemperature).plus(" - ").plus(String.format("%.1f", highestTemperature)).plus("°C")).build(),
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    else -> null
                }
            )
        }
    }

}