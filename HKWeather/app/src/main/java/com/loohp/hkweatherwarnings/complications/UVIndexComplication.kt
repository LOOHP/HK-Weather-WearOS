package com.loohp.hkweatherwarnings.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.RangedValueComplicationData.Companion.TYPE_RATING
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.weather.UVIndexType
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class UVIndexComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val uvindex = 3F
        val icon = Icon.createWithResource(this, R.mipmap.uvindex)
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build()
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build()
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = uvindex,
                    min = 0F,
                    max = 11F,
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build()
                )
                    .setValueType(TYPE_RATING)
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
            val uvindex = weatherInfo.uvIndex.coerceIn(0F, 11F)
            val uvIndexType = UVIndexType.getByValue(uvindex)
            val icon = Icon.createWithResource(this, R.mipmap.uvindex)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.UVINDEX.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build()
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.0f", uvindex).plus(" ").plus(if (Registry.getInstance(this).language == "en") uvIndexType.en else uvIndexType.zh)).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex).plus(" ").plus(if (Registry.getInstance(this).language == "en") uvIndexType.en else uvIndexType.zh)).build()
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.RANGED_VALUE -> {
                        RangedValueComplicationData.Builder(
                            value = uvindex,
                            min = 0F,
                            max = 11F,
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", uvindex)).build()
                        )
                            .setValueType(TYPE_RATING)
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