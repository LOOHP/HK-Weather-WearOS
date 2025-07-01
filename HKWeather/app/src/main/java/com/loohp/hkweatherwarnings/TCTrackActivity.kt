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

package com.loohp.hkweatherwarnings

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.clamp
import com.loohp.hkweatherwarnings.weather.TropicalCycloneInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class TrackImageTransformation : Transformation {

    companion object {
        private const val heightRatio: Float = 691F / 720F
    }

    override val cacheKey: String = javaClass.name

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val output = createBitmap(input.width, (input.width * heightRatio).roundToInt(), input.config?: Bitmap.Config.ARGB_8888)
        output.applyCanvas {
            drawBitmap(input, 0F, 0F, paint)
        }
        return output
    }

    override fun equals(other: Any?) = other is TrackImageTransformation

    override fun hashCode() = javaClass.hashCode()
}

class ZoomedTrackImageTransformation : Transformation {

    companion object {
        private const val heightRatio: Float = 685F / 720F
    }

    override val cacheKey: String = javaClass.name

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val output = createBitmap(input.width, (input.width * heightRatio).roundToInt(), input.config?: Bitmap.Config.ARGB_8888)
        output.applyCanvas {
            drawBitmap(input, 0F, 0F, paint)
        }
        return output
    }

    override fun equals(other: Any?) = other is ZoomedTrackImageTransformation

    override fun hashCode() = javaClass.hashCode()
}

@Stable
class TCTrackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TCTrackElement(this)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TCTrackElement(instance: TCTrackActivity) {
    var tropicalCyclones: List<TropicalCycloneInfo>? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            try {
                val data = Registry.getInstance(instance).getTropicalCycloneInfo(instance).get(60, TimeUnit.SECONDS)
                    .filter { it.trackStaticImageUrl != null }
                    .sortedBy { it.displayOrder }
                tropicalCyclones = data
                for (cyclone in data) {
                    launch { instance.imageLoader.execute(ImageRequest.Builder(instance).data(cyclone.trackStaticImageUrl).transformations(TrackImageTransformation(), RoundedCornersTransformation(10F)).build()) }
                    if (cyclone.trackStaticZoomImageUrl != null) {
                        launch { instance.imageLoader.execute(ImageRequest.Builder(instance).data(cyclone.trackStaticZoomImageUrl).transformations(ZoomedTrackImageTransformation(), RoundedCornersTransformation(10F)).build()) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                instance.runOnUiThread {
                    Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Unable to download data" else "無法下載資料", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    HKWeatherTheme {
        val cyclones = tropicalCyclones
        if (cyclones == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeText(
                    modifier = Modifier.fillMaxWidth(0.8F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    text = if (Registry.getInstance(instance).language == "en") "Loading storm tracks..." else "正在載入風暴路徑..."
                )
            }
        } else if (cyclones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeText(
                    modifier = Modifier.fillMaxWidth(0.8F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    text = if (Registry.getInstance(instance).language == "en") "There are currently no tropical cyclones entering or forming within the area bounded by 7-36N and 100-140E." else "目前沒有熱帶氣旋進入北緯7至36度，東經100至140度的範圍，或在此範圍內形成"
                )
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            val state = rememberPagerState { cyclones.size + 1 }
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current
            val scrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }
            val scrollReachedEnd by remember { derivedStateOf { state.canScrollBackward != state.canScrollForward } }
            val mutex by remember { mutableStateOf(Mutex()) }
            val zoomInImage: MutableMap<Int, Boolean> = remember { mutableMapOf() }
            var refresher by remember { mutableFloatStateOf(0F) }

            LaunchedEffect (scrollInProgress, scrollReachedEnd) {
                if (scrollReachedEnd) {
                    delay(50)
                    if (scrollInProgress) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
            LaunchedEffect (Unit) {
                focusRequester.requestFocus()
            }

            Box (
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                HorizontalPager (
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            scope.launch {
                                mutex.withLock {
                                    state.animateScrollToPage(
                                        page = state.currentPage + (if (it.horizontalScrollPixels > 0) 1 else -1),
                                        animationSpec = TweenSpec(
                                            durationMillis = 500,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            }
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable(),
                    state = state
                ) {
                    if (it >= cyclones.size) {
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp, 0.dp),
                            model = ImageRequest.Builder(instance).size(720, 209).data(R.mipmap.tctrack_legend).build(),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Legend" else "圖例"
                        )
                    } else {
                        val cyclone = cyclones[it]
                        Box (
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val pageOffset =
                                        ((state.currentPage - it) + state.currentPageOffsetFraction).absoluteValue
                                    alpha = lerp(0.3f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                                    val scale = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                                    scaleX = scale
                                    scaleY = scale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            @Suppress("UNUSED_EXPRESSION") refresher
                            SubcomposeAsyncImage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zoomable(
                                        state = rememberZoomableState()
                                    ),
                                loading = {
                                    Box (
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(25.dp),
                                            color = Color.LightGray,
                                            strokeWidth = 3.dp,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                },
                                alignment = Alignment.Center,
                                model = if (zoomInImage[it] == true) {
                                    ImageRequest.Builder(instance).size(720, 684).data(cyclone.trackStaticZoomImageUrl).transformations(ZoomedTrackImageTransformation(), RoundedCornersTransformation(10F)).build()
                                } else {
                                    ImageRequest.Builder(instance).size(720, 691).data(cyclone.trackStaticImageUrl).transformations(TrackImageTransformation(), RoundedCornersTransformation(10F)).build()
                                },
                                contentScale = ContentScale.Crop,
                                contentDescription = if (Registry.getInstance(instance).language == "en") cyclone.nameEn.plus(" (Zoomed)") else cyclone.nameZh.plus(" (放大)")
                            )
                            AutoResizeText(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding((LocalConfiguration.current.screenWidthDp / 4F).dp, 10.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSizeRange = FontSizeRange(
                                    min = 1F.sp,
                                    max = StringUtils.scaledSize(17F, instance).sp.clamp(max = 17.dp)
                                ),
                                maxLines = 1,
                                text = if (Registry.getInstance(instance).language == "en") cyclone.nameEn else cyclone.nameZh
                            )
                            if (cyclone.trackStaticZoomImageUrl != null) {
                                Button(
                                    onClick = {
                                        zoomInImage[it] = !(zoomInImage[it]?: false)
                                        refresher++
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(0.dp, 0.dp, 0.dp, 16.dp)
                                        .width(StringUtils.scaledSize(30, instance).dp)
                                        .height(StringUtils.scaledSize(30, instance).dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.5F),
                                        contentColor = Color(0xFFFFFFFF)
                                    ),
                                    content = {
                                        Icon(
                                            modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                            painter = painterResource(if (zoomInImage[it] == true) R.drawable.baseline_zoom_out_24 else R.drawable.baseline_zoom_in_24),
                                            contentDescription = if (Registry.getInstance(instance).language == "en") "Toggle Playback" else "開始/暫停播放",
                                            tint = Color(0xFFFFFFFF)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier
                        .height(17.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..cyclones.size) {
                        val pageOffset = ((state.currentPage - i) + state.currentPageOffsetFraction).absoluteValue
                        val size = lerp(5f, 7f, 1f - pageOffset.coerceIn(0f, 1f))
                        val brightness = lerp(0.25f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(brightness, brightness, brightness))
                                .alpha(0.8F)
                                .size(size.dp)
                        )
                    }
                }
            }
        }
    }
}