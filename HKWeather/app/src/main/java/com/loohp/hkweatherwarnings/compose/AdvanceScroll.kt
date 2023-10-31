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

package com.loohp.hkweatherwarnings.compose

import android.content.res.Configuration
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun Modifier.fullPageVerticalScrollbar(
    state: ScrollState,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = 0.8f
): Modifier = composed {
    val configuration = LocalConfiguration.current
    var scrollOffsetViewPort by remember { mutableFloatStateOf(0F) }
    val animatedScrollOffsetViewPort by animateFloatAsState(
        targetValue = scrollOffsetViewPort,
        animationSpec = TweenSpec(durationMillis = 100, easing = LinearEasing),
        label = ""
    )

    drawWithContent {
        drawContent()

        val contentOffset = state.value
        val viewPortLength = size.height
        val contentLength = (viewPortLength + state.maxValue).coerceAtLeast(0.001f)

        if (viewPortLength < contentLength) {
            val indicatorLength = viewPortLength / contentLength
            val indicatorThicknessPx = indicatorThickness.toPx()
            val halfIndicatorThicknessPx = (indicatorThickness.value / 2F).dp.toPx()
            scrollOffsetViewPort = contentOffset / contentLength

            if (configuration.screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK == Configuration.SCREENLAYOUT_ROUND_YES) {
                val topLeft = Offset(halfIndicatorThicknessPx, halfIndicatorThicknessPx)
                val size = Size(configuration.screenWidthDp.dp.toPx() - indicatorThicknessPx, configuration.screenHeightDp.dp.toPx() - indicatorThicknessPx)
                val style = Stroke(width = indicatorThicknessPx, cap = StrokeCap.Round)
                drawArc(
                    startAngle = -30F,
                    sweepAngle = 60F,
                    useCenter = false,
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
                drawArc(
                    startAngle = -30F + animatedScrollOffsetViewPort * 60F,
                    sweepAngle = indicatorLength * 60F,
                    useCenter = false,
                    color = indicatorColor,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
            } else {
                val cornerRadius = CornerRadius(indicatorThicknessPx / 2F)
                val topLeft = Offset(configuration.screenWidthDp.dp.toPx() - indicatorThicknessPx, viewPortLength * 0.125F)
                val size = Size(indicatorThicknessPx, viewPortLength * 0.75F)
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(topLeft.x, topLeft.y + animatedScrollOffsetViewPort * size.height),
                    size = Size(size.width, size.height * indicatorLength),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

fun Modifier.fullPageVerticalScrollbar(
    state: LazyListState,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = 0.8f
): Modifier = composed {
    val configuration = LocalConfiguration.current
    val actualItemLength: MutableMap<Int, Int> = remember { mutableMapOf() }
    var totalItemCount by remember { mutableIntStateOf(0) }
    var indicatorLength by remember { mutableFloatStateOf(0F) }
    var scrollOffsetViewPort by remember { mutableFloatStateOf(0F) }
    val animatedIndicatorLength by animateFloatAsState(
        targetValue = indicatorLength,
        animationSpec = TweenSpec(durationMillis = 300, easing = LinearEasing),
        label = ""
    )
    val animatedScrollOffsetViewPort by animateFloatAsState(
        targetValue = scrollOffsetViewPort,
        animationSpec = TweenSpec(durationMillis = 100, easing = LinearEasing),
        label = ""
    )

    drawWithContent {
        drawContent()

        val viewPortLength = size.height
        val itemsVisible = state.layoutInfo.visibleItemsInfo
        if (totalItemCount != state.layoutInfo.totalItemsCount) {
            actualItemLength.clear()
            totalItemCount = state.layoutInfo.totalItemsCount
        }
        itemsVisible.forEach { actualItemLength[it.index] = it.size }
        val visibleItemsLength = itemsVisible.sumOf { it.size }.toFloat() - state.firstVisibleItemScrollOffset
        val knownLength = actualItemLength.entries.sumOf { it.value }
        val knownAmount = actualItemLength.values.count()
        val knownAverageItemLength = knownLength / knownAmount
        val contentOffset = (0 until state.firstVisibleItemIndex).sumOf { actualItemLength.getOrDefault(it, knownAverageItemLength) }.toFloat() + state.firstVisibleItemScrollOffset
        val contentLength = knownLength + (state.layoutInfo.totalItemsCount - knownAmount) * (knownLength / knownAmount)

        if (viewPortLength < contentLength) {
            indicatorLength = (if (itemsVisible.last { it.offset < viewPortLength }.index >= state.layoutInfo.totalItemsCount) 1F - (contentOffset / contentLength) else visibleItemsLength / contentLength).coerceAtLeast(0.05F)
            val indicatorThicknessPx = indicatorThickness.toPx()
            val halfIndicatorThicknessPx = (indicatorThickness.value / 2F).dp.toPx()
            scrollOffsetViewPort = contentOffset / contentLength

            if (configuration.screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK == Configuration.SCREENLAYOUT_ROUND_YES) {
                val topLeft = Offset(halfIndicatorThicknessPx, halfIndicatorThicknessPx)
                val size = Size(configuration.screenWidthDp.dp.toPx() - indicatorThicknessPx, configuration.screenHeightDp.dp.toPx() - indicatorThicknessPx)
                val style = Stroke(width = indicatorThicknessPx, cap = StrokeCap.Round)
                val startAngle = (-30F + animatedScrollOffsetViewPort * 60F).coerceIn(-30F, 30F)
                drawArc(
                    startAngle = -30F,
                    sweepAngle = 60F,
                    useCenter = false,
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
                drawArc(
                    startAngle = startAngle,
                    sweepAngle = (animatedIndicatorLength * 60F).coerceAtMost(60F - (startAngle + 30F)),
                    useCenter = false,
                    color = indicatorColor,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
            } else {
                val cornerRadius = CornerRadius(indicatorThicknessPx / 2F)
                val topLeft = Offset(configuration.screenWidthDp.dp.toPx() - indicatorThicknessPx, viewPortLength * 0.125F)
                val size = Size(indicatorThicknessPx, viewPortLength * 0.75F)
                val startHeight = (topLeft.y + animatedScrollOffsetViewPort * size.height).coerceIn(topLeft.y, topLeft.y + size.height)
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(topLeft.x, startHeight),
                    size = Size(size.width, (size.height * animatedIndicatorLength).coerceAtMost(size.height - (startHeight - topLeft.y))),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

data class FullPageScrollBarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorColor: Color = Color.LightGray,
    val alpha: Float? = null,
    val alphaAnimationSpec: AnimationSpec<Float>? = null
)

fun Modifier.fullPageVerticalLazyScrollbar(
    state: LazyListState,
    scrollbarConfigFullPage: FullPageScrollBarConfig = FullPageScrollBarConfig()
) = this
    .fullPageVerticalScrollbar(
        state,
        indicatorThickness = scrollbarConfigFullPage.indicatorThickness,
        indicatorColor = scrollbarConfigFullPage.indicatorColor,
        alpha = scrollbarConfigFullPage.alpha ?: 0.8f
    )


fun Modifier.fullPageVerticalScrollWithScrollbar(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfigFullPage: FullPageScrollBarConfig = FullPageScrollBarConfig()
) = this
    .fullPageVerticalScrollbar(
        state,
        indicatorThickness = scrollbarConfigFullPage.indicatorThickness,
        indicatorColor = scrollbarConfigFullPage.indicatorColor,
        alpha = scrollbarConfigFullPage.alpha ?: 0.8f
    )
    .verticalScroll(state, enabled, flingBehavior, reverseScrolling)