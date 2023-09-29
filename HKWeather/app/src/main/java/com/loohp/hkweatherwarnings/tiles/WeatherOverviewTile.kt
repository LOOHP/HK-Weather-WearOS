package com.loohp.hkweatherwarnings.tiles

import android.text.format.DateFormat
import android.util.Pair
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.DynamicDataBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.shared.Shared.Companion.FRESHNESS_TIME
import com.loohp.hkweatherwarnings.shared.Shared.Companion.currentTips
import com.loohp.hkweatherwarnings.shared.Shared.Companion.currentWarnings
import com.loohp.hkweatherwarnings.shared.Shared.Companion.currentWeatherInfo
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clampSp
import com.loohp.hkweatherwarnings.utils.timeZone
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo
import com.loohp.hkweatherwarnings.weather.WeatherStatusIcon
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool

private const val RESOURCES_VERSION = "0"
private var tileUpdatedTime: Long = 0
private var state = false

class WeatherOverviewTile : TileService() {

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        if (tileUpdatedTime < currentWeatherInfo.getLastSuccessfulUpdateTime(this)) {
            getUpdater(this).requestUpdate(javaClass)
        }
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        @Suppress("UnstableApiUsage")
        return Futures.submit(Callable {
            val isReload = requestParams.currentState.keyToValueMapping.containsKey(AppDataKey<DynamicBuilders.DynamicString>("reload"))
            val data = Triple(
                currentWeatherInfo.getLatestValue(this, ForkJoinPool.commonPool(), isReload),
                currentWarnings.getLatestValue(this, ForkJoinPool.commonPool(), isReload),
                currentTips.getLatestValue(this, ForkJoinPool.commonPool(), isReload)
            )
            val weatherInfo = data.first.get()
            val warnings = data.second.get()
            val tips = data.third.get()
            val updateSuccess = currentWeatherInfo.isLastUpdateSuccess(this)
            val updateTime = currentWeatherInfo.getLastSuccessfulUpdateTime(this)
            tileUpdatedTime = System.currentTimeMillis()

            var element: LayoutElementBuilders.LayoutElement =
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setClickable(
                                ModifiersBuilders.Clickable.Builder()
                                    .setOnClick(
                                        ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                    .setClassName(MainActivity::class.java.name)
                                                    .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.MAIN.name))
                                                    .setPackageName(packageName)
                                                    .build()
                                            ).build()
                                    )
                                    .setId("open")
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        buildContent(updateTime, updateSuccess, weatherInfo, warnings, tips)
                    )
                    .build()

            element = if (state) {
                state = false
                element
            } else {
                state = true
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .addContent(element)
                    .build()
            }

            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_TIME.invoke(this))
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder().setLayout(
                            LayoutElementBuilders.Layout.Builder().setRoot(
                                element
                            ).build()
                        ).build()
                    ).build()
                ).build()
        }, ForkJoinPool.commonPool())
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val bundle = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
            .addIdToImageMapping("reload", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.reload)
                        .build()
                ).build()
            )
            .addIdToImageMapping("humidity", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.humidity)
                        .build()
                ).build()
            )
            .addIdToImageMapping("uvindex", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.uvindex)
                        .build()
                ).build()
            )
            .addIdToImageMapping("highest", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.highest)
                        .build()
                ).build()
            )
            .addIdToImageMapping("lowest", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.lowest)
                        .build()
                ).build()
            )
            .addIdToImageMapping("umbrella", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.umbrella)
                        .build()
                ).build()
            )
            .addIdToImageMapping("gps", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.gps)
                        .build()
                ).build()
            )
        for (type in WeatherStatusIcon.values()) {
            bundle.addIdToImageMapping(type.iconName, ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(type.iconId)
                        .build()
                ).build()
            )
        }
        return Futures.immediateFuture(bundle.build())
    }

    private fun buildTitle(updateTime: Long, updateSuccess: Boolean, weatherInfo: CurrentWeatherInfo?): LayoutElementBuilders.LayoutElement {
        var lastUpdateText = (if (Registry.getInstance(this).language == "en") "Updated: " else "更新時間: ").plus(
            DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(Date(updateTime)))
        if (!updateSuccess) {
            lastUpdateText = lastUpdateText.plus(if (Registry.getInstance(this).language == "en") " (Update Failed)" else " (無法更新)")
        }
        val text = if (weatherInfo == null) "-" else weatherInfo.weatherStation

        val titleLayout = if (Registry.getInstance(this).location.first == "GPS") {
            LayoutElementBuilders.Row.Builder()
                .setWidth(DimensionBuilders.wrap())
                .setHeight(DimensionBuilders.wrap())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(30F, this)).build())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(text)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(
                                            DimensionBuilders.SpProp.Builder().setValue(clampSp(this, UnitUtils.dpToSp(this, StringUtils.findOptimalSp(this, text, StringUtils.scaledSize(230, this), 1, 1F, 17F)), dpMax = 18F)).build()
                                        )
                                        .setWeight(
                                            LayoutElementBuilders.FontWeightProp.Builder()
                                                .setValue(LayoutElementBuilders.FONT_WEIGHT_BOLD).build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Image.Builder()
                        .setWidth(
                            DimensionBuilders.DpProp.Builder(
                                StringUtils.scaledSize(
                                    16F,
                                    this
                                )
                            ).build())
                        .setHeight(
                            DimensionBuilders.DpProp.Builder(
                                StringUtils.scaledSize(
                                    16F,
                                    this
                                )
                            ).build())
                        .setResourceId("gps")
                        .build()
                )
                .build()
        } else {
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.wrap())
                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(23F, this)).build())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(text)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(
                                    DimensionBuilders.SpProp.Builder().setValue(clampSp(this, StringUtils.findOptimalSp(this, text, StringUtils.scaledSize(250, this), 1, 1F, 17F), dpMax = 18F)).build()
                                )
                                .setWeight(
                                    LayoutElementBuilders.FontWeightProp.Builder()
                                        .setValue(LayoutElementBuilders.FONT_WEIGHT_BOLD).build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.wrap())
            .setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(
                LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                    .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .build()
            )
            .setVerticalAlignment(
                LayoutElementBuilders.VerticalAlignmentProp.Builder()
                    .setValue(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.wrap())
                    .setHeight(DimensionBuilders.wrap())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        titleLayout
                    )
                    .addContent(
                        LayoutElementBuilders.Row.Builder()
                            .setWidth(DimensionBuilders.wrap())
                            .setHeight(DimensionBuilders.wrap())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText(lastUpdateText.plus(" "))
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(
                                                DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 11F)).build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Image.Builder()
                                    .setWidth(
                                        DimensionBuilders.DpProp.Builder(
                                            StringUtils.scaledSize(
                                                12F,
                                                this
                                            )
                                        ).build())
                                    .setHeight(
                                        DimensionBuilders.DpProp.Builder(
                                            StringUtils.scaledSize(
                                                12F,
                                                this
                                            )
                                        ).build())
                                    .setModifiers(
                                        ModifiersBuilders.Modifiers.Builder()
                                            .setClickable(
                                                ModifiersBuilders.Clickable.Builder()
                                                    .setId("refresh")
                                                    .setOnClick(
                                                        ActionBuilders.LoadAction.Builder()
                                                            .setRequestState(
                                                                StateBuilders.State.Builder()
                                                                    .addKeyToValueMapping(
                                                                        AppDataKey<DynamicBuilders.DynamicString>("reload"),
                                                                        DynamicDataBuilders.DynamicDataValue.fromString("")
                                                                    )
                                                                    .build()
                                                            )
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .setResourceId("reload")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildContent(updateTime: Long, updateSuccess: Boolean, weatherInfo: CurrentWeatherInfo?, warnings: Map<WeatherWarningsType, String?>, tips: List<Pair<String, Long>>): LayoutElementBuilders.LayoutElement {
        return if (weatherInfo == null) {
            LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setTop(DimensionBuilders.DpProp.Builder(30F).build())
                                .setBottom(DimensionBuilders.DpProp.Builder(30F).build())
                                .setStart(DimensionBuilders.DpProp.Builder(15F).build())
                                .setEnd(DimensionBuilders.DpProp.Builder(15F).build())
                                .build()
                        ).build()
                )
                .addContent(
                    buildTitle(updateTime, updateSuccess, null)
                )
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(
                            LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                                .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .build()
                        )
                        .setVerticalAlignment(
                            LayoutElementBuilders.VerticalAlignmentProp.Builder()
                                .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(if (Registry.getInstance(this).language == "en") "Unable to get weather information." else "無法取得天氣資訊")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(
                                            DimensionBuilders.SpProp.Builder().setValue(15F).build()
                                        )
                                        .build()
                                )
                                .setMaxLines(Int.MAX_VALUE)
                                .build()
                        ).build()
                )
                .build()
        } else {
            val today = LocalDate.now(Shared.HK_TIMEZONE.toZoneId())
            val forecastIndex = if (weatherInfo.forecastInfo[0].date.equals(today)) 1 else 0

            val layout = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(
                    LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                        .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .build()
                )
                .setVerticalAlignment(
                    LayoutElementBuilders.VerticalAlignmentProp.Builder()
                        .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setTop(DimensionBuilders.DpProp.Builder(25F).build())
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            buildTitle(updateTime, updateSuccess, weatherInfo)
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.expand())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Image.Builder()
                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(55F, this)).build())
                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(55F, this)).build())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setTop(DimensionBuilders.DpProp.Builder(5F).build())
                                                .setBottom(DimensionBuilders.DpProp.Builder(5F).build())
                                                .setStart(DimensionBuilders.DpProp.Builder(5F).build())
                                                .setEnd(DimensionBuilders.DpProp.Builder(5F).build())
                                                .build()
                                        ).build()
                                )
                                .setResourceId(weatherInfo.weatherIcon.iconName)
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(String.format("%.1f", weatherInfo.currentTemperature).plus("°"))
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(
                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 35F)).build()
                                        )
                                        .setWeight(
                                            LayoutElementBuilders.FontWeightProp.Builder()
                                                .setValue(LayoutElementBuilders.FONT_WEIGHT_BOLD).build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setTop(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(130F, this)).build())
                                        .build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Row.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .addContent(
                                    LayoutElementBuilders.Image.Builder()
                                        .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                        .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setResourceId("highest")
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setText(String.format("%.1f", weatherInfo.highestTemperature).plus("°  "))
                                        .setFontStyle(
                                            LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 13F)).build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Image.Builder()
                                        .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                        .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setResourceId("lowest")
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setText(String.format("%.1f", weatherInfo.lowestTemperature).plus("°  "))
                                        .setFontStyle(
                                            LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 13F)).build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Image.Builder()
                                        .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                        .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(14F, this)).build())
                                        .setResourceId("umbrella")
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setText(String.format("%.0f", weatherInfo.chanceOfRain).plus("%"))
                                        .setFontStyle(
                                            LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 13F)).build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Row.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.FORECAST.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setTop(DimensionBuilders.DpProp.Builder(10F).build())
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Column.Builder()
                                        .setWidth(DimensionBuilders.wrap())
                                        .setHeight(DimensionBuilders.wrap())
                                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                        .setModifiers(
                                            ModifiersBuilders.Modifiers.Builder()
                                                .setPadding(
                                                    ModifiersBuilders.Padding.Builder()
                                                        .setStart(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .setEnd(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(weatherInfo.forecastInfo[forecastIndex + 0].dayOfWeek.getDisplayName(TextStyle.SHORT, if (Registry.getInstance(this).language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Image.Builder()
                                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setResourceId(weatherInfo.forecastInfo[forecastIndex + 0].weatherIcon.iconName)
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(String.format("%.0f", weatherInfo.forecastInfo[forecastIndex + 0].lowestTemperature).plus("-").plus(String.format("%.0f", weatherInfo.forecastInfo[0].highestTemperature)))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Column.Builder()
                                        .setWidth(DimensionBuilders.wrap())
                                        .setHeight(DimensionBuilders.wrap())
                                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                        .setModifiers(
                                            ModifiersBuilders.Modifiers.Builder()
                                                .setPadding(
                                                    ModifiersBuilders.Padding.Builder()
                                                        .setStart(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .setEnd(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(weatherInfo.forecastInfo[forecastIndex + 1].dayOfWeek.getDisplayName(TextStyle.SHORT, if (Registry.getInstance(this).language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Image.Builder()
                                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setResourceId(weatherInfo.forecastInfo[forecastIndex + 1].weatherIcon.iconName)
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(String.format("%.0f", weatherInfo.forecastInfo[forecastIndex + 1].lowestTemperature).plus("-").plus(String.format("%.0f", weatherInfo.forecastInfo[1].highestTemperature)))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Column.Builder()
                                        .setWidth(DimensionBuilders.wrap())
                                        .setHeight(DimensionBuilders.wrap())
                                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                        .setModifiers(
                                            ModifiersBuilders.Modifiers.Builder()
                                                .setPadding(
                                                    ModifiersBuilders.Padding.Builder()
                                                        .setStart(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .setEnd(DimensionBuilders.DpProp.Builder(2F).build())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(weatherInfo.forecastInfo[forecastIndex + 2].dayOfWeek.getDisplayName(TextStyle.SHORT, if (Registry.getInstance(this).language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Image.Builder()
                                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                                .setResourceId(weatherInfo.forecastInfo[forecastIndex + 2].weatherIcon.iconName)
                                                .build()
                                        )
                                        .addContent(
                                            LayoutElementBuilders.Text.Builder()
                                                .setText(String.format("%.0f", weatherInfo.forecastInfo[forecastIndex + 2].lowestTemperature).plus("-").plus(String.format("%.0f", weatherInfo.forecastInfo[2].highestTemperature)))
                                                .setFontStyle(
                                                    LayoutElementBuilders.FontStyle.Builder()
                                                        .setSize(
                                                            DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(230F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(80F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color.DarkGray.toArgb()).build()
                                ).build()
                        ).build()
                ).addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(230F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(weatherInfo.currentHumidity / 100F * 80F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color(0xFF3CB0FF).toArgb()).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setTop(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(172F, this)).build())
                                        .setStart(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(34F, this)).build())
                                        .build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Image.Builder()
                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                .setResourceId("humidity")
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.HUMIDITY.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(-230F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(-80F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color.DarkGray.toArgb()).build()
                                ).build()
                        ).build()
                ).addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(-230F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(weatherInfo.uvIndex.coerceAtLeast(0F).coerceAtMost(11F) / 11F * -80F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color(0xFFFF9625).toArgb()).build()
                                ).build()
                        ).build()
                ).addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setTop(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(172F, this)).build())
                                        .setEnd(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(34F, this)).build())
                                        .build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Image.Builder()
                                .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(18F, this)).build())
                                .setResourceId("uvindex")
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.UVINDEX.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        ).build()
                ).addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(35F, this)).build())
                                .setHeight(DimensionBuilders.expand())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.HUMIDITY.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            if (updateSuccess) {
                layout.addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(35F, this)).build())
                                .setHeight(DimensionBuilders.expand())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.UVINDEX.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
            for ((i, value) in warnings.keys.withIndex()) {
                layout.addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(317F + (i * 6F)).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(0.1F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color(value.color).toArgb()).build()
                                ).build()
                        ).build()
                    )
            }
            if (warnings.isNotEmpty()) {
                layout.addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                        .addContent(
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledWidth(80F, this)).build())
                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledHeight(50F, this)).build())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.WARNINGS.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
            for (i in tips.indices) {
                layout.addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(-317F - (i * 6F)).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            LayoutElementBuilders.ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(-0.1F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.DpProp.Builder(8.5F).build()
                                )
                                .setColor(
                                    ColorBuilders.ColorProp.Builder(Color(0xFFFF0000).toArgb()).build()
                                ).build()
                        ).build()
                )
            }
            if (tips.isNotEmpty()) {
                layout.addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                        .addContent(
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.DpProp.Builder(StringUtils.scaledWidth(80F, this)).build())
                                .setHeight(DimensionBuilders.DpProp.Builder(StringUtils.scaledHeight(50F, this)).build())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setClickable(
                                            ModifiersBuilders.Clickable.Builder()
                                                .setOnClick(
                                                    ActionBuilders.LaunchAction.Builder()
                                                        .setAndroidActivity(
                                                            ActionBuilders.AndroidActivity.Builder()
                                                                .setClassName(MainActivity::class.java.name)
                                                                .addKeyToExtraMapping("launchSection", ActionBuilders.stringExtra(Section.TIPS.name))
                                                                .setPackageName(packageName)
                                                                .build()
                                                        ).build()
                                                )
                                                .setId("open")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
            layout.build()
        }
    }

}