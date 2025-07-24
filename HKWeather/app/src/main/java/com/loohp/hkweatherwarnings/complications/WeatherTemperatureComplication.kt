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
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.weather.WeatherStatusIcon
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class WeatherTemperatureComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val temperature = 25F
        val icon = Icon.createWithResource(this, R.mipmap.pic51)
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.0f", temperature).plus("°")).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build()
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build()
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.ICON).setAmbientImage(icon).build(),
                    contentDescription = PlainComplicationText.Builder(if (Registry.getInstance(this).language == "en") WeatherStatusIcon._51.descriptionEn else WeatherStatusIcon._51.descriptionZh).build()
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
            val temperature = weatherInfo.currentTemperature
            val icon = Icon.createWithResource(this, weatherInfo.weatherIcon.iconId)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.MAIN.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.0f", temperature).plus("°")).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build()
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.1f", temperature).plus("°C")).build()
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.SMALL_IMAGE -> {
                        SmallImageComplicationData.Builder(
                            smallImage = SmallImage.Builder(icon, SmallImageType.ICON).setAmbientImage(icon).build(),
                            contentDescription = PlainComplicationText.Builder(if (Registry.getInstance(this).language == "en") weatherInfo.weatherIcon.descriptionEn else weatherInfo.weatherIcon.descriptionZh).build()
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