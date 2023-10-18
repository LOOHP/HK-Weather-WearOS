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
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.utils.nextOccurrenceIsCloserThan
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class MoonriseMoonsetComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val timeFormat = DateTimeFormatter.ofPattern(DateFormat.getTimeFormat(this).let { if (it is SimpleDateFormat) it.toPattern() else "HH:mm" })
        val time = timeFormat.format(LocalTime.of(7, 0))
        val icon = Icon.createWithResource(this, R.mipmap.moonrise)
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(time).build(),
                    contentDescription = PlainComplicationText.Builder(time).build(),
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(time).build(),
                    contentDescription = PlainComplicationText.Builder(time).build(),
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
            val timeFormat = DateTimeFormatter.ofPattern(DateFormat.getTimeFormat(this).let { if (it is SimpleDateFormat) it.toPattern() else "HH:mm" })
            val (time, icon) = if (weatherInfo.moonriseTime.nextOccurrenceIsCloserThan(weatherInfo.moonsetTime)) {
                timeFormat.format(weatherInfo.moonriseTime) to Icon.createWithResource(this, R.mipmap.moonrise)
            } else {
                timeFormat.format(weatherInfo.moonsetTime) to Icon.createWithResource(this, R.mipmap.moonset)
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.MOON.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(time).build(),
                            contentDescription = PlainComplicationText.Builder(time).build(),
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(time).build(),
                            contentDescription = PlainComplicationText.Builder(time).build(),
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