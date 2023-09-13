package com.loohp.hkweatherwarnings.tiles

import android.text.format.DateFormat
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
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
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.timeZone
import com.loohp.hkweatherwarnings.warnings.WeatherWarningsType
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

private const val RESOURCES_VERSION = "0"
private var lastWarnings: Set<WeatherWarningsType> = emptySet()
private var state = false

class WeatherWarningsTile : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.submit(Callable {
            var warnings = Registry.getInstance(this).getActiveWarnings(this).get(9, TimeUnit.SECONDS)
            val updateSuccess = if (warnings == null) {
                warnings = lastWarnings
                false
            } else {
                lastWarnings = warnings
                true
            }

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
                        buildTitle(updateSuccess)
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
        val bundle = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
            .addIdToImageMapping("reload", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.reload)
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

    private fun buildTitle(updateSuccess: Boolean): LayoutElementBuilders.LayoutElement {
        var lastUpdateText = (if (Registry.getInstance(this).language == "en") "Updated: " else "更新時間: ").plus(
            DateFormat.getTimeFormat(this).timeZone(Shared.HK_TIMEZONE).format(Date()))
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
                                                        ActionBuilders.LoadAction.Builder().setRequestState(
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

    private fun buildContent(warnings: Set<WeatherWarningsType>): LayoutElementBuilders.LayoutElement {
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
                                    DimensionBuilders.SpProp.Builder().setValue(15F).build()
                                )
                                .build()
                        )
                        .setMaxLines(Int.MAX_VALUE)
                        .build()
                } else {
                    val images = warnings.stream().map {
                        LayoutElementBuilders.Image.Builder()
                            .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_CROP)
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
                            .setResourceId(it.iconName)
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
                                                        StringUtils.scaledSize(60F, this)
                                                    ).build())
                                                .setHeight(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(60F, this)
                                                    ).build())
                                                .build()
                                        )
                                        .addContent(
                                            images[1]
                                                .setWidth(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(60F, this)
                                                    ).build())
                                                .setHeight(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(60F, this)
                                                    ).build())
                                                .build()
                                        )
                                        .build()
                                )
                            }
                            3 -> {
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
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .setHeight(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .build()
                                        )
                                        .addContent(
                                            images[1]
                                                .setWidth(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .setHeight(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .build()
                                        )
                                        .addContent(
                                            images[2]
                                                .setWidth(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .setHeight(
                                                    DimensionBuilders.DpProp.Builder(
                                                        StringUtils.scaledSize(50F, this)
                                                    ).build())
                                                .build()
                                        )
                                        .build()
                                )
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
                                                    StringUtils.scaledSize(
                                                        40F,
                                                        this
                                                    )
                                                ).build())
                                            .setHeight(
                                                DimensionBuilders.DpProp.Builder(
                                                    StringUtils.scaledSize(
                                                        40F,
                                                        this
                                                    )
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