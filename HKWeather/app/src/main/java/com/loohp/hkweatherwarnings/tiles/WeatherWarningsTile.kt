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

package com.loohp.hkweatherwarnings.tiles

import android.text.format.DateFormat
import androidx.wear.protolayout.ActionBuilders
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
import com.loohp.hkweatherwarnings.shared.Shared.Companion.currentWarnings
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.timeZone
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import kotlin.streams.toList

private const val RESOURCES_VERSION = "0"
private var tileUpdatedTime: Long = 0
private var state = false

class WeatherWarningsTile : TileService() {

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        if (tileUpdatedTime < currentWarnings.getLastSuccessfulUpdateTime(this)) {
            getUpdater(this).requestUpdate(javaClass)
        }
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        @Suppress("UnstableApiUsage")
        return Futures.submit(Callable {
            val isReload = requestParams.currentState.keyToValueMapping.containsKey(AppDataKey<DynamicBuilders.DynamicString>("reload"))
            val future = currentWarnings.getLatestValue(this, ForkJoinPool.commonPool(), isReload)
            val warnings = future.orIntermediateValue
            val updating = !future.isDone
            val updateSuccess = currentWarnings.isLastUpdateSuccess(this)
            val updateTime = currentWarnings.getLastSuccessfulUpdateTime(this)
            tileUpdatedTime = System.currentTimeMillis()

            var element: LayoutElementBuilders.LayoutElement =
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
                    .addContent(
                        buildTitle(updateTime, updateSuccess, updating)
                    )
                    .addContent(
                        buildContent(warnings)
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
            .addIdToImageMapping("reloading", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.reloading)
                        .build()
                ).build()
            )
        for (type in WeatherWarningsType.values()) {
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

    private fun buildTitle(updateTime: Long, updateSuccess: Boolean, updating: Boolean): LayoutElementBuilders.LayoutElement {
        var lastUpdateText = (if (Registry.getInstance(this).language == "en") "Updated: " else "更新時間: ").plus(
            DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(Date(updateTime)))
        if (!updateSuccess) {
            lastUpdateText = lastUpdateText.plus(if (Registry.getInstance(this).language == "en") " (Update Failed)" else " (無法更新)")
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
                            .setText(if (Registry.getInstance(this).language == "en") "Weather Warnings" else "天氣警告")
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
                                    .setResourceId(if (updating) "reloading" else "reload")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildContent(warnings: Map<WeatherWarningsType, String?>): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
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
                if (warnings.isEmpty()) {
                    LayoutElementBuilders.Text.Builder()
                        .setText(if (Registry.getInstance(this).language == "en") "There are currently no active warning signals." else "目前沒有任何天氣警告信號")
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(
                                    DimensionBuilders.SpProp.Builder().setValue(UnitUtils.dpToSp(this, 15F)).build()
                                )
                                .build()
                        )
                        .setMaxLines(Int.MAX_VALUE)
                        .build()
                } else {
                    val images = warnings.entries.stream().map {
                        val modifiers = ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setTop(DimensionBuilders.DpProp.Builder(5F).build())
                                    .setBottom(DimensionBuilders.DpProp.Builder(5F).build())
                                    .setStart(DimensionBuilders.DpProp.Builder(5F).build())
                                    .setEnd(DimensionBuilders.DpProp.Builder(5F).build())
                                    .build()
                            )
                        if (it.value != null) {
                            modifiers.setClickable(
                                ModifiersBuilders.Clickable.Builder()
                                    .setOnClick(
                                        ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                    .setClassName(MainActivity::class.java.name)
                                                    .addKeyToExtraMapping("imageDrawable", ActionBuilders.AndroidIntExtra.Builder().setValue(it.key.iconId).build())
                                                    .addKeyToExtraMapping("imageWidth", ActionBuilders.AndroidIntExtra.Builder().setValue(StringUtils.scaledSize(60, this)).build())
                                                    .addKeyToExtraMapping("imageDescription", ActionBuilders.AndroidStringExtra.Builder().setValue(if (Registry.getInstance(this).language == "en") it.key.nameEn else it.key.nameZh).build())
                                                    .addKeyToExtraMapping("text", ActionBuilders.AndroidStringExtra.Builder().setValue(it.value!!).build())
                                                    .addKeyToExtraMapping("warningInfo", ActionBuilders.AndroidIntExtra.Builder().setValue(1).build())
                                                    .setPackageName(packageName)
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .setId("warningInfo")
                                    .build()
                            )
                        }
                        LayoutElementBuilders.Image.Builder()
                            .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
                            .setModifiers(modifiers.build())
                            .setResourceId(it.key.iconName)
                    }.toList()
                    val element = LayoutElementBuilders.Column.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setHorizontalAlignment(
                            LayoutElementBuilders.HorizontalAlignmentProp.Builder()
                                .setValue(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .build()
                        )
                    when (images.size) {
                        1 -> {
                            element.addContent(
                                LayoutElementBuilders.Row.Builder()
                                    .setWidth(DimensionBuilders.wrap())
                                    .setHeight(DimensionBuilders.wrap())
                                    .setVerticalAlignment(
                                        LayoutElementBuilders.VerticalAlignmentProp.Builder()
                                            .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .build()
                                    )
                                    .addContent(
                                        images[0]
                                            .setWidth(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(70F, this)
                                                ).build())
                                            .setHeight(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(70F, this)
                                                ).build())
                                            .build()
                                    )
                                    .build()
                            )
                        }
                        2 -> {
                            element.addContent(
                                LayoutElementBuilders.Row.Builder()
                                    .setWidth(DimensionBuilders.wrap())
                                    .setHeight(DimensionBuilders.wrap())
                                    .setVerticalAlignment(
                                        LayoutElementBuilders.VerticalAlignmentProp.Builder()
                                            .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .build()
                                    )
                                    .addContent(
                                        images[0]
                                            .setWidth(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(65F, this)
                                                ).build())
                                            .setHeight(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(65F, this)
                                                ).build())
                                            .build()
                                    )
                                    .addContent(
                                        images[1]
                                            .setWidth(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(65F, this)
                                                ).build())
                                            .setHeight(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(65F, this)
                                                ).build())
                                            .build()
                                    )
                                    .build()
                            )
                        }
                        in 3..5 -> {
                            for (i in images.indices step 3) {
                                val row = LayoutElementBuilders.Row.Builder()
                                    .setWidth(DimensionBuilders.wrap())
                                    .setHeight(DimensionBuilders.wrap())
                                    .setVerticalAlignment(
                                        LayoutElementBuilders.VerticalAlignmentProp.Builder()
                                            .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .build()
                                    )
                                for (u in i until (i + 3).coerceAtMost(images.size)) {
                                    row.addContent(images[u]
                                        .setWidth(
                                            DimensionBuilders.DpProp.Builder(
                                                StringUtils.scaledSize(60F, this)
                                            ).build())
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(
                                                StringUtils.scaledSize(60F, this)
                                            ).build())
                                        .build())
                                }
                                element.addContent(row.build())
                            }
                        }
                        else -> {
                            for (i in images.indices step 4) {
                                val row = LayoutElementBuilders.Row.Builder()
                                    .setWidth(DimensionBuilders.wrap())
                                    .setHeight(DimensionBuilders.wrap())
                                    .setVerticalAlignment(
                                        LayoutElementBuilders.VerticalAlignmentProp.Builder()
                                            .setValue(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .build()
                                    )
                                for (u in i until (i + 4).coerceAtMost(images.size)) {
                                    row.addContent(images[u]
                                        .setWidth(
                                            DimensionBuilders.DpProp.Builder(
                                                StringUtils.scaledSize(40F, this)
                                            ).build())
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(
                                                StringUtils.scaledSize(40F, this)
                                            ).build())
                                        .build())
                                }
                                element.addContent(row.build())
                            }
                        }
                    }
                    element.build()
                }
            )
            .build()
    }

}