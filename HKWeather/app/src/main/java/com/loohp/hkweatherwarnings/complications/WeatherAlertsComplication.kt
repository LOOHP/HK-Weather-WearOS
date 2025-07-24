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
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class WeatherAlertsComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val text = if (Registry.getInstance(this).language == "en") "1 weather warning in force" else "1個天氣警告現正生效"
        val icon = Icon.createWithResource(this, R.mipmap.alert)
        return when (type) {
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder(text).build()
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
            val warnings = Shared.currentWarnings.getLatestValue(this, ForkJoinPool.commonPool()).orIntermediateValue
            val tips = Shared.currentTips.getLatestValue(this, ForkJoinPool.commonPool()).orIntermediateValue
            if (weatherInfo == null || tips == null || warnings == null) {
                listener.onComplicationData(null)
                return@execute
            }
            val (text, icon) = if (warnings.isEmpty() && tips.isEmpty()) {
                (if (Registry.getInstance(this).language == "en") "No weather alerts" else "沒有任何天氣警告或提示") to Icon.createWithResource(this, weatherInfo.weatherIcon.iconId)
            } else {
                val warningText = if (warnings.isEmpty()) "" else warnings.size.toString().plus(if (Registry.getInstance(this).language == "en") " warning" else "個警告")
                val tipsText = if (tips.isEmpty()) "" else tips.size.toString().plus(if (Registry.getInstance(this).language == "en") " special tip" else "個特別提示")
                val text = if (warningText.isNotEmpty() && tipsText.isNotEmpty()) {
                    warningText.plus(if (Registry.getInstance(this).language == "en") " & " else "及").plus(tipsText)
                } else if (warningText.isNotEmpty()) {
                    warningText
                } else {
                    tipsText
                }
                text to Icon.createWithResource(this, R.mipmap.alert)
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.WARNINGS.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(text).build(),
                            contentDescription = PlainComplicationText.Builder(text).build()
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