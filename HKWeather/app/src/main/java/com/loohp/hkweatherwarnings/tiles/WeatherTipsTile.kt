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

package com.loohp.hkweatherwarnings.tiles

import android.text.format.DateFormat
import android.util.Pair
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.LoadAction
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
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
import com.loohp.hkweatherwarnings.utils.ConnectionUtils
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.floorToInt
import com.loohp.hkweatherwarnings.utils.timeZone
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import kotlin.math.roundToInt

private const val RESOURCES_VERSION = "1"
private var tileUpdatedTime: Long = 0
private var currentIndex: Int = 0
private var state = false


class WeatherTipsTile : TileService() {

    override fun onRecentInteractionEventsAsync(events: MutableList<EventBuilders.TileInteractionEvent>): ListenableFuture<Void?> {
        return Futures.submit(Callable {
            for (event in events) {
                if (event.eventType == EventBuilders.TileInteractionEvent.ENTER) {
                    if (tileUpdatedTime < currentTips.getLastSuccessfulUpdateTime(this)) {
                        getUpdater(this).requestUpdate(javaClass)
                    }
                    Shared.startBackgroundService(this)
                }
            }
            null
        }, ForkJoinPool.commonPool())
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.submit(Callable {
            if (requestParams.currentState.keyToValueMapping.containsKey(AppDataKey<DynamicString>("next"))) {
                currentIndex++
            }
            val isReload = requestParams.currentState.keyToValueMapping.containsKey(AppDataKey<DynamicString>("reload"))
            val future = currentTips.getLatestValue(this, ForkJoinPool.commonPool(), isReload)
            val tips = future.orIntermediateValue
            val updating = !future.isDone
            val updateSuccess = currentTips.isLastUpdateSuccess(this)
            val updateTime = currentTips.getLastSuccessfulUpdateTime(this)
            tileUpdatedTime = System.currentTimeMillis()

            val content = buildContent(tips)
            val elementBuilder = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(
                            if (tips.size <= 1) {
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
                            } else {
                                ModifiersBuilders.Clickable.Builder()
                                    .setOnClick(
                                        LoadAction.Builder()
                                            .setRequestState(
                                                StateBuilders.State.Builder()
                                                    .addKeyToValueMapping(
                                                        AppDataKey<DynamicString>("next"),
                                                        DynamicDataBuilders.DynamicDataValue.fromString("")
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .setId("open")
                                    .build()
                            }
                        )
                        .build()
                )
                .addContent(
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
                                )
                                .build()
                        )
                        .addContent(
                            buildTitle(updateSuccess, updateTime, updating)
                        )
                        .addContent(
                            content[0]
                        )
                        .build()
                )
            if (content.size > 1) {
                elementBuilder.addContent(content[1])
            }

            val element = if (state) {
                state = false
                elementBuilder.build()
            } else {
                state = true
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .addContent(elementBuilder.build())
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
        return Futures.submit(Callable {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
                .addIdToImageMapping("reload", ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.mipmap.reload)
                            .build()
                    ).build()
                )
                .addIdToImageMapping("reloading", ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.mipmap.reloading)
                            .build()
                    ).build()
                ).build()
        }, ForkJoinPool.commonPool())
    }

    @OptIn(ProtoLayoutExperimental::class)
    private fun buildTitle(updateSuccess: Boolean, updatedTime: Long, updating: Boolean): LayoutElementBuilders.LayoutElement {
        var lastUpdateText = (if (Registry.getInstance(this).language == "en") "Updated: " else "更新時間: ").plus(
            DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(Date(updatedTime)))
        if (!updateSuccess) {
            lastUpdateText = lastUpdateText.plus(if (Registry.getInstance(this).language == "en") " (Failed)" else " (無法更新)")
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
                        LayoutElementBuilders.Text.Builder()
                            .setText(if (Registry.getInstance(this).language == "en") "Special Weather Tips" else "特別天氣提示")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(
                                        DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, StringUtils.scaledSize(17F, this))).build()
                                    )
                                    .setWeight(
                                        LayoutElementBuilders.FontWeightProp.Builder()
                                            .setValue(LayoutElementBuilders.FONT_WEIGHT_BOLD).build()
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
                                                        LoadAction.Builder()
                                                            .setRequestState(
                                                                StateBuilders.State.Builder()
                                                                    .addKeyToValueMapping(
                                                                        AppDataKey("reload"),
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
                                    .setResourceId(if (updating) "reloading" else "reload")
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Box.Builder()
                            .setWidth(DimensionBuilders.dp(UnitUtils.pixelsToDp(this, ScreenSizeUtils.getScreenWidth(this).toFloat() * 0.75F)))
                            .setHeight(DimensionBuilders.wrap())
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText(if (!updateSuccess) {
                                        when (ConnectionUtils.isBackgroundRestricted(this)) {
                                            ConnectionUtils.BackgroundRestrictionType.RESTRICT_BACKGROUND_STATUS -> {
                                                if (Registry.getInstance(this).language == "en") "Background Internet Restricted - Data Saver" else "背景網絡存取被限制 - 數據節省器"
                                            }
                                            ConnectionUtils.BackgroundRestrictionType.POWER_SAVE_MODE -> {
                                                if (Registry.getInstance(this).language == "en") "Background Internet Restricted - Power Saving" else "背景網絡存取被限制 - 省電模式"
                                            }
                                            ConnectionUtils.BackgroundRestrictionType.LOW_POWER_STANDBY -> {
                                                if (Registry.getInstance(this).language == "en") "Background Internet Restricted - Low Power Standby" else "背景網絡存取被限制 - 低耗電待機"
                                            }
                                            else -> {
                                                ""
                                            }
                                        }
                                    } else {
                                        ""
                                    })
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(
                                                DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 9F)).build()
                                            )
                                            .setColor(
                                                ColorBuilders.ColorProp.Builder(Color(0xFFFF6A6A).toArgb()).build()
                                            )
                                            .build()
                                    )
                                    .setOverflow(TEXT_OVERFLOW_MARQUEE)
                                    .setMarqueeIterations(-1)
                                    .build()
                            ).build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildContent(tips: List<Pair<String, Long>>): List<LayoutElementBuilders.LayoutElement> {
        val layouts: MutableList<LayoutElementBuilders.LayoutElement> = ArrayList(2)
        val tip = if (tips.isEmpty()) {
            null
        } else {
            currentIndex = if (currentIndex < tips.size) currentIndex else 0
            tips[currentIndex]
        }
        val tipText = if (tip == null) (if (Registry.getInstance(this).language == "en") "There are currently no active special weather tips." else "目前沒有任何特別天氣提示") else tip.first
        val heightMultiplier = if (Registry.getInstance(this).language == "en") 0.45F else 0.35F
        val maxLines = (ScreenSizeUtils.getScreenHeight(this) * (if (Registry.getInstance(this).language == "en") 0.0155F else 0.0133F)).floorToInt()
        val tipTextSize = UnitUtils.dpToSp(this, StringUtils.findOptimalSpForHeight(this, tipText, ScreenSizeUtils.getScreenWidth(this) - 50, (ScreenSizeUtils.getScreenHeight(this).toFloat() * heightMultiplier).roundToInt(), 11F, 15F))

        layouts.add(
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.wrap())
                .setHeight(DimensionBuilders.DpProp.Builder(UnitUtils.pixelsToDp(this, ScreenSizeUtils.getScreenHeight(this) * 0.45F)).build())
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
                        .setText(tipText)
                        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(
                                    DimensionBuilders.SpProp.Builder().setValue(tipTextSize).build()
                                )
                                .build()
                        )
                        .setMaxLines(maxLines)
                        .build()
                )
                .build()
        )

        if (tip != null) {
            val date = Date(tip.second)
            val lastUpdateText = DateFormat.getDateFormat(this).timeZone(Shared.HK_TIMEZONE).format(date)
                .plus(" ")
                .plus(DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(date))

            layouts.add(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setBottom(DimensionBuilders.DpProp.Builder(3F).build())
                                    .build()
                            )
                            .build()
                    )
                    .setHorizontalAlignment(
                        LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                            .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .build()
                    )
                    .setVerticalAlignment(
                        LayoutElementBuilders.VerticalAlignmentProp.Builder()
                            .setValue(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Column.Builder()
                            .setWidth(DimensionBuilders.expand())
                            .setHeight(DimensionBuilders.wrap())
                            .setHorizontalAlignment(
                                LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                                    .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText(lastUpdateText)
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(
                                                DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 11F)).build()
                                            )
                                            .build()
                                    )
                                    .setMaxLines(Int.MAX_VALUE)
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Text.Builder()
                                    .setText((currentIndex + 1).toString().plus(" / ").plus(tips.size))
                                    .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                            .setSize(
                                                DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 11F)).build()
                                            )
                                            .build()
                                    )
                                    .setMaxLines(Int.MAX_VALUE)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        }

        return layouts
    }

}