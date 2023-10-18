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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clamp
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

class RadarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadarElement(this)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadarElement(instance: RadarActivity) {
    HKWeatherTheme {
        val focusRequester = remember { FocusRequester() }
        var currentPosition by remember { mutableStateOf(1) }
        var playback by remember { mutableStateOf(true) }
        var zoom by remember { mutableStateOf(false) }
        var showLegend by remember { mutableStateOf(false) }
        val ready: MutableMap<Int, Boolean> = remember { mutableStateMapOf() }

        var currentProgress by remember { mutableStateOf(0F) }
        val progressAnimation by animateFloatAsState(
            targetValue = currentProgress,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "LoadingProgressAnimation"
        )
        LaunchedEffect (Unit) {
            while (true) {
                currentProgress = ready.values.sumOf { (if (it) 1 else 0).toInt() } / 20F
                if (currentProgress >= 0.999999F) {
                    break
                }
                delay(200)
            }
        }

        LaunchedEffect (Unit) {
            focusRequester.requestFocus()
            while (true) {
                if (playback) {
                    var pos = currentPosition
                    if (pos >= 20) {
                        pos = 1
                    } else {
                        pos++
                    }
                    if (ready.getOrDefault(pos, false)) {
                        currentPosition = pos
                    }
                }
                delay(750)
            }
        }

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
            if (showLegend) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(0.dp, 10.dp),
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
                        model = ImageRequest.Builder(instance).size(640).data(R.mipmap.radar_64_background).build(),
                        contentDescription = "",
                    )
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = ImageRequest.Builder(instance).size(640).data(R.mipmap.radar_64_gridline).build(),
                        contentDescription = "",
                    )
                    for (i in 1..20) {
                        @Suppress("UnnecessaryVariable")
                        val index = i
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = {
                                ready[index] = true
                            },
                            model = ImageRequest.Builder(instance).size(640).data("https://pda.weather.gov.hk/locspc/android_data/radar/rad_064_640/${index}_64km.png").build(),
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
                            progress = progressAnimation
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
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 14.dp),
                    text = currentPosition.toString().plus(" / 20")
                )
            }
            if (!zoom) {
                if (!showLegend) {
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
                        showLegend = !showLegend
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
            }
        }
    }
}