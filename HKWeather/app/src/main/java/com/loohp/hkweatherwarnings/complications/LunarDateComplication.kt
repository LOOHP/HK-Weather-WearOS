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
import com.loohp.hkweatherwarnings.shared.Shared
import java.time.LocalDate
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class LunarDateComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("癸卯").build(),
                    contentDescription = PlainComplicationText.Builder("癸卯").build(),
                )
                    .setTitle(PlainComplicationText.Builder("八月十九").build())
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("癸卯, 兔年八月十九").build(),
                    contentDescription = PlainComplicationText.Builder("癸卯, 兔年八月十九").build(),
                )
                    .setTapAction(null)
                    .build()
            }
            else -> null
        }
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        ForkJoinPool.commonPool().execute {
            val lunarDate = Shared.convertedLunarDates.getValue(LocalDate.now(Shared.HK_TIMEZONE.toZoneId()), this, ForkJoinPool.commonPool()).get()
            if (lunarDate == null) {
                listener.onComplicationData(null)
                return@execute
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.DATE.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        val topText = if (lunarDate.hasClimatology()) lunarDate.climatology else lunarDate.year
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(topText).build(),
                            contentDescription = PlainComplicationText.Builder(topText).build(),
                        )
                            .setTitle(PlainComplicationText.Builder(lunarDate.date).build())
                            .setTapAction(pendingIntent)
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(lunarDate.toString()).build(),
                            contentDescription = PlainComplicationText.Builder(lunarDate.toString()).build(),
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