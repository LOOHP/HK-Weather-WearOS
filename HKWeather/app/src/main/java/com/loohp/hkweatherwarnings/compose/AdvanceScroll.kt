package com.loohp.hkweatherwarnings.compose

import android.content.res.Configuration
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp


fun Modifier.scrollbar(
    state: ScrollState,
    direction: Orientation,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = 0.8f
): Modifier = composed {
    val configuration = LocalConfiguration.current

    drawWithContent {
        drawContent()

        val contentOffset = state.value
        val viewPortLength = if (direction == Orientation.Vertical) size.height else size.width
        val contentLength = (viewPortLength + state.maxValue).coerceAtLeast(0.001f)
        val indicatorLength = viewPortLength / contentLength
        val indicatorThicknessPx = indicatorThickness.toPx()
        val halfIndicatorThicknessPx = (indicatorThickness.value / 2F).dp.toPx()
        val scrollOffsetViewPort = contentOffset / contentLength

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
                startAngle = -30F + scrollOffsetViewPort * 60F,
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
                topLeft = Offset(topLeft.x, topLeft.y + scrollOffsetViewPort * size.height),
                size = Size(size.width, size.height * indicatorLength),
                cornerRadius = cornerRadius
            )
        }
    }
}

data class ScrollBarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorColor: Color = Color.LightGray,
    val alpha: Float? = null,
    val alphaAnimationSpec: AnimationSpec<Float>? = null
)

fun Modifier.verticalScrollWithScrollbar(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollBarConfig = ScrollBarConfig()
) = this
    .scrollbar(
        state, Orientation.Vertical,
        indicatorThickness = scrollbarConfig.indicatorThickness,
        indicatorColor = scrollbarConfig.indicatorColor,
        alpha = scrollbarConfig.alpha ?: 0.8f
    )
    .verticalScroll(state, enabled, flingBehavior, reverseScrolling)



fun Modifier.horizontalScrollWithScrollbar(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollBarConfig = ScrollBarConfig()
) = this
    .scrollbar(
        state, Orientation.Horizontal,
        indicatorThickness = scrollbarConfig.indicatorThickness,
        indicatorColor = scrollbarConfig.indicatorColor,
        alpha = scrollbarConfig.alpha ?: 0.8f
    )
    .horizontalScroll(state, enabled, flingBehavior, reverseScrolling)