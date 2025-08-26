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

package com.loohp.hkweatherwarnings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clamp
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable


@Stable
class RadarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadarElement(this)
        }
    }

}

enum class RadarVariant(
    val radarRange: Int,
    val radarHeight: Int,
    val framesRange: IntRange,
    val urlProvider: (Int) -> String,
    val background: Int,
    val gridline: Int
) {
    RADAR_64KM_2KM(
        radarRange = 64,
        radarHeight = 2,
        framesRange = 1..20,
        urlProvider = { "https://pda.weather.gov.hk/locspc/android_data/radar/v2/rad_064_640_2km/${it}_64km.png" },
        background = R.mipmap.radar_64_background,
        gridline = R.mipmap.radar_64_gridline
    ),
    RADAR_64KM_3KM(
        radarRange = 64,
        radarHeight = 3,
        framesRange = 1..20,
        urlProvider = { "https://pda.weather.gov.hk/locspc/android_data/radar/v2/rad_064_640/${it}_64km.png" },
        background = R.mipmap.radar_64_background,
        gridline = R.mipmap.radar_64_gridline
    ),
    RADAR_128KM_3KM(
        radarRange = 128,
        radarHeight = 3,
        framesRange = 1..20,
        urlProvider = { "https://pda.weather.gov.hk/locspc/android_data/radar/v2/rad_128_640/${it}_128km.png" },
        background = R.mipmap.radar_128_background,
        gridline = R.mipmap.radar_128_gridline
    ),
    RADAR_256KM_3KM(
        radarRange = 256,
        radarHeight = 3,
        framesRange = 1..20,
        urlProvider = { "https://pda.weather.gov.hk/locspc/android_data/radar/v2/rad_256_640/${it}_256km.png" },
        background = R.mipmap.radar_256_background,
        gridline = R.mipmap.radar_256_gridline
    );

    val frameSize = framesRange.count()
}

private enum class RadarElementDisplayMode {
    RADAR, LEGEND, CHOOSER
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadarElement(instance: RadarActivity) {
    HKWeatherTheme {
        var currentMode by remember { mutableStateOf(RadarElementDisplayMode.RADAR) }
        var radarVariant by remember { mutableStateOf(RadarVariant.RADAR_64KM_2KM) }
        val focusRequester = remember { FocusRequester() }
        var currentPosition by remember { mutableIntStateOf(1) }
        var playback by remember { mutableStateOf(true) }
        var zoom by remember { mutableStateOf(false) }
        val ready: MutableMap<String, Boolean> = remember { mutableStateMapOf() }

        var currentProgress by remember { mutableFloatStateOf(0F) }
        val progressAnimation by animateFloatAsState(
            targetValue = currentProgress,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "LoadingProgressAnimation"
        )

        LaunchedEffect (radarVariant) {
            while (true) {
                currentProgress = ready.asSequence()
                    .filter { (k) -> k.startsWith(radarVariant.name) }
                    .sumOf { (_, v) -> if (v) 1 else 0 } / radarVariant.frameSize.toFloat()
                if (currentProgress >= 0.999999F) {
                    break
                }
                delay(200)
            }
        }

        LaunchedEffect (Unit) {
            while (true) {
                if (playback) {
                    var pos = currentPosition
                    if (pos >= 20) {
                        pos = 1
                    } else {
                        pos++
                    }
                    if (ready.getOrDefault("${radarVariant.name}-$pos", false)) {
                        currentPosition = pos
                    }
                }
                delay(750)
            }
        }

        LaunchedEffect (currentMode) {
            if (currentMode != RadarElementDisplayMode.CHOOSER) {
                focusRequester.requestFocus()
            }
        }

        AnimatedContent(
            targetState = currentMode,
            label = "CurrentMode"
        ) { mode ->
            when (mode) {
                RadarElementDisplayMode.CHOOSER -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(StringUtils.scaledSize(5F, instance).dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (variant in RadarVariant.entries) {
                            Button(
                                onClick = {
                                    currentMode = RadarElementDisplayMode.RADAR
                                    radarVariant = variant
                                },
                                modifier = Modifier
                                    .width(StringUtils.scaledSize(120, instance).dp)
                                    .height(StringUtils.scaledSize(35, instance).dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = MaterialTheme.colors.primary
                                ),
                                content = {
                                    AutoResizeText(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.primary,
                                        fontSizeRange = FontSizeRange(
                                            min = 1F.sp,
                                            max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                                        ),
                                        fontWeight = FontWeight.Bold.takeIf { radarVariant == variant },
                                        maxLines = 1,
                                        text = if (Registry.getInstance(instance).language == "en") "${variant.radarRange} km (${variant.radarHeight} km height)" else "${variant.radarRange}公里 (${variant.radarHeight}公里高)"
                                    )
                                }
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onRotaryScrollEvent {
                                if (it.verticalScrollPixels > 0) {
                                    if (currentPosition >= 20) {
                                        currentPosition = 1
                                    } else {
                                        currentPosition++
                                    }
                                } else {
                                    if (currentPosition <= 1) {
                                        currentPosition = 20
                                    } else {
                                        currentPosition--
                                    }
                                }
                                playback = false
                                true
                            }
                            .focusRequester(focusRequester)
                            .focusable()
                    ) {
                        if (mode == RadarElementDisplayMode.LEGEND) {
                            Image(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp, 20.dp),
                                painter = painterResource(R.mipmap.radar_legend),
                                contentDescription = if (Registry.getInstance(instance).language == "en") "Legend" else "圖例"
                            )
                        } else {
                            val zoomPadding by remember { derivedStateOf { if (zoom) 0F else UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getMinScreenSize(instance) * 0.15F) } }
                            val animatedZoomPadding by animateFloatAsState(
                                targetValue = zoomPadding,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "AnimatedZoomPadding"
                            )
                            var modifier = Modifier
                                .fillMaxSize()
                                .padding(animatedZoomPadding.dp)
                            if (zoom) {
                                modifier = modifier.zoomable(
                                    state = rememberZoomableState(),
                                    onClick = {
                                        zoom = !zoom
                                    }
                                )
                            } else {
                                modifier = modifier.combinedClickable(
                                    onClick = {
                                        zoom = !zoom
                                    }
                                )
                            }

                            Box(
                                modifier = modifier
                            ) {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = ImageRequest.Builder(instance).size(640).data(radarVariant.background).build(),
                                    contentDescription = "",
                                )
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = ImageRequest.Builder(instance).size(640).data(radarVariant.gridline).build(),
                                    contentDescription = "",
                                )
                                for (i in radarVariant.framesRange) {
                                    @Suppress("UnnecessaryVariable")
                                    val index = i
                                    AsyncImage(
                                        modifier = Modifier.fillMaxSize(),
                                        onSuccess = { ready["${radarVariant.name}-$index"] = true },
                                        model = ImageRequest.Builder(instance).size(640).data(radarVariant.urlProvider.invoke(index)).build(),
                                        contentDescription = if (Registry.getInstance(instance).language == "en") "Radar Image $index / 20" else "雷達圖像 $index / 20",
                                        alpha = if (currentPosition == index) 1F else 0F
                                    )
                                }
                                if (currentProgress < 0.999999F) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth(),
                                        color = Color(0xFFF9DE09),
                                        trackColor = Color(0xFF797979),
                                        progress = { progressAnimation }
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(if (zoom) Alignment.BottomCenter else Alignment.TopCenter)
                                    .padding(0.dp, 6.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontWeight = if (zoom) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14F.sp.clamp(max = 14.dp),
                                text = currentPosition.toString().plus(" / 20")
                            )
                        }
                        if (!zoom) {
                            if (mode == RadarElementDisplayMode.RADAR) {
                                Button(
                                    onClick = {
                                        playback = !playback
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(0.dp, 0.dp, 0.dp, 1.dp)
                                        .width(StringUtils.scaledSize(30, instance).dp)
                                        .height(StringUtils.scaledSize(30, instance).dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.secondary,
                                        contentColor = Color(0xFFFFFFFF)
                                    ),
                                    content = {
                                        if (playback) {
                                            Icon(
                                                modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                                painter = painterResource(R.drawable.baseline_pause_24),
                                                contentDescription = if (Registry.getInstance(instance).language == "en") "Toggle Playback" else "開始/暫停播放",
                                                tint = Color(0xFFFFFFFF)
                                            )
                                        } else {
                                            Icon(
                                                modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = if (Registry.getInstance(instance).language == "en") "Toggle Playback" else "開始/暫停播放",
                                                tint = Color(0xFFFFFFFF)
                                            )
                                        }
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    currentMode = RadarElementDisplayMode.LEGEND
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(0.dp, 0.dp, 1.dp, 0.dp)
                                    .width(StringUtils.scaledSize(30, instance).dp)
                                    .height(StringUtils.scaledSize(30, instance).dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                content = {
                                    Image(
                                        modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                        painter = painterResource(R.mipmap.radar_legend),
                                        contentDescription = if (Registry.getInstance(instance).language == "en") "Toggle Show Legend" else "顯示/隱藏圖例"
                                    )
                                }
                            )
                            Button(
                                onClick = {
                                    currentMode = RadarElementDisplayMode.CHOOSER
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(1.dp, 0.dp, 0.dp, 0.dp)
                                    .width(StringUtils.scaledSize(30, instance).dp)
                                    .height(StringUtils.scaledSize(30, instance).dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary,
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                content = {
                                    Icon(
                                        modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = if (Registry.getInstance(instance).language == "en") "Choose Radar Image Variant" else "選擇雷達圖像模式",
                                        tint = Color(0xFFFFFFFF)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}