package com.loohp.hkweatherwarnings.tiles

import android.text.format.DateFormat
import android.util.Pair
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.LoadAction
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.TitleActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.timeZone
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val RESOURCES_VERSION = "0"
private var currentTips: List<Pair<String, Long>> = emptyList()
private var currentUpdateSuccess: Boolean = false
private var currentUpdatedTime: Long = 0
private var currentIndex: Int = 0
private var state = false


class WeatherTipsTile : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.submit(Callable {
            val tips: List<Pair<String, Long>>
            if (currentUpdateSuccess && requestParams.currentState.keyToValueMapping.containsKey(AppDataKey<DynamicString>("next"))) {
                tips = currentTips
                currentIndex++
            } else {
                val newTips = Registry.getInstance(this).getWeatherTips(this).get(9, TimeUnit.SECONDS)
                if (newTips == null) {
                    tips = currentTips
                    currentUpdateSuccess = false
                } else {
                    currentTips = newTips
                    tips = newTips
                    currentUpdateSuccess = true
                }
                currentUpdatedTime = System.currentTimeMillis()
            }

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
                            buildTitle(currentUpdateSuccess, currentUpdatedTime)
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
                .setFreshnessIntervalMillis(900000)
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
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
                .addIdToImageMapping("reload", ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.mipmap.reload)
                            .build()
                    ).build()
                ).build())
    }

    private fun buildTitle(updateSuccess: Boolean, updatedTime: Long): LayoutElementBuilders.LayoutElement {
        var lastUpdateText = (if (Registry.getInstance(this).language == "en") "Updated: " else "更新時間: ").plus(
            DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(Date(updatedTime)))
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
                            .setText(if (Registry.getInstance(this).language == "en") "Special Weather Tips" else "特別天氣提示")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(
                                        DimensionBuilders.SpProp.Builder().setValue(17F).build()
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
                                                DimensionBuilders.SpProp.Builder().setValue(11F).build()
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
                                                        LoadAction.Builder().setRequestState(
                                                            StateBuilders.State.Builder().build()).build()
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
        val tipTextSize = StringUtils.findOptimalSpForHeight(this, tipText, ScreenSizeUtils.getScreenWidth(this) - 50, (ScreenSizeUtils.getScreenHeight(this).toFloat() * heightMultiplier).roundToInt(), 1F, 15F)

        layouts.add(
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
                        .setText(tipText)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(
                                    DimensionBuilders.SpProp.Builder().setValue(tipTextSize).build()
                                )
                                .build()
                        )
                        .setMaxLines(Int.MAX_VALUE)
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
                    .setWidth(DimensionBuilders.wrap())
                    .setHeight(DimensionBuilders.expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setBottom(DimensionBuilders.DpProp.Builder(3F).build())
                                    .build()
                            ).build()
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
                            .setWidth(DimensionBuilders.wrap())
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
                                                DimensionBuilders.SpProp.Builder().setValue(11F).build()
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
                                                DimensionBuilders.SpProp.Builder().setValue(11F).build()
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