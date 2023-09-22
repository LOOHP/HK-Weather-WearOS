package com.loohp.hkweatherwarnings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clamp
import kotlinx.coroutines.delay

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
                .combinedClickable(
                    onClick = {
                        zoom = !zoom
                    }
                )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            if (zoom) 0.dp else UnitUtils.pixelsToDp(
                                instance,
                                ScreenSizeUtils.getMinScreenSize(instance) * 0.15F
                            ).dp
                        )
                ) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.mipmap.radar_64_background),
                        contentDescription = ""
                    )
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.mipmap.radar_64_gridline),
                        contentDescription = ""
                    )
                    for (i in 1..20) {
                        @Suppress("UnnecessaryVariable")
                        val index = i
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = {
                                ready[index] = true
                            },
                            model = "https://pda.weather.gov.hk/locspc/android_data/radar/rad_064_320/${index}_64km.png",
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Radar Image $index / 20" else "雷達圖像 $index / 20",
                            alpha = if (currentPosition == index) 1F else 0F
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(if (zoom) Alignment.BottomCenter else Alignment.TopCenter)
                        .padding(0.dp, 6.dp),
                    textAlign = TextAlign.Center,
                    color = if (zoom) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
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
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Toggle Playback" else "開始/暫停播放"
                        )
                    }
                )
            }
        }
    }
}