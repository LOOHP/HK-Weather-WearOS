package com.loohp.hkweatherwarnings

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.FutureWithProgress
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clamp
import com.loohp.hkweatherwarnings.weather.RainfallMapsInfo
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.concurrent.ForkJoinPool

enum class RainfallMapMode(val nameZh: String, val nameEn: String) {

    PAST_ONE_HOUR("過去1小時", "Last Hour"),
    PAST_TWENTY_FOUR_HOUR("過去24小時", "Past 24-hour"),
    TODAY("今天", "Today"),
    YESTERDAY("昨天", "Yesterday");

    companion object {
        private val values = values()
    }

    fun next(): RainfallMapMode = values[(ordinal + 1) % values.size]
}

class RainfallMapImageTransformation : Transformation {

    override val cacheKey: String = javaClass.name

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val output = createBitmap(input.width, input.width, input.config?: Bitmap.Config.ARGB_8888)
        output.applyCanvas {
            drawBitmap(input, 0F, 0F, paint)
        }
        return output
    }

    override fun equals(other: Any?) = other is RainfallMapImageTransformation

    override fun hashCode() = javaClass.hashCode()
}

class RainfallMapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RainfallMapElement(this)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RainfallMapElement(instance: RainfallMapActivity) {
    HKWeatherTheme {
        var rainfallMapsInfo: RainfallMapsInfo? by remember { mutableStateOf(null) }

        var rainfallMapsFuture: FutureWithProgress<RainfallMapsInfo>? by remember { mutableStateOf(null) }
        var currentProgress by remember { mutableStateOf(0F) }

        val focusRequester = remember { FocusRequester() }
        var currentMode by remember { mutableStateOf(RainfallMapMode.PAST_ONE_HOUR) }
        var currentPosition by remember { mutableStateOf(0) }
        var zoom by remember { mutableStateOf(false) }

        val haptic = LocalHapticFeedback.current

        LaunchedEffect (rainfallMapsInfo, currentMode) {
            currentPosition = (rainfallMapsInfo?.past1HourUrls?.size ?: 1) - 1
        }
        LaunchedEffect (Unit) {
            ForkJoinPool.commonPool().execute {
                rainfallMapsFuture = Registry.getInstance(instance).getRainfallMaps(instance)
                rainfallMapsInfo = rainfallMapsFuture!!.get()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (rainfallMapsInfo == null) {
                val progressAnimation by animateFloatAsState(
                    targetValue = currentProgress,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "LoadingProgressAnimation"
                )
                LaunchedEffect (Unit) {
                    while (true) {
                        currentProgress = rainfallMapsFuture?.progress?: 0F
                        delay(500)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AutoResizeText(
                        modifier = Modifier.fillMaxWidth(0.8F),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSizeRange = FontSizeRange(
                            min = TextUnit(1F, TextUnitType.Sp),
                            max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                        ),
                        text = if (Registry.getInstance(instance).language == "en") "Loading Isohyet Charts..." else "正在載入等雨量線圖..."
                    )
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(15, instance).dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.7F)
                            .padding(25.dp, 0.dp),
                        color = Color(0xFF42D3FF),
                        trackColor = Color(0xFF797979),
                        progress = progressAnimation
                    )
                }
            } else {
                LaunchedEffect (Unit) {
                    focusRequester.requestFocus()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            if (it.verticalScrollPixels > 0) {
                                if (currentPosition < rainfallMapsInfo!!.past1HourUrls.size - 1) {
                                    currentPosition++
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else {
                                if (currentPosition > 0) {
                                    currentPosition--
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable()
                ) {
                    var modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            if (zoom) 0.dp else UnitUtils.pixelsToDp(
                                instance,
                                ScreenSizeUtils.getMinScreenSize(instance) * 0.15F
                            ).dp
                        )
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
                        when (currentMode) {
                            RainfallMapMode.PAST_ONE_HOUR -> {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .requiredSize(25.dp),
                                            color = Color.LightGray,
                                            strokeWidth = 3.dp,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    },
                                    model = ImageRequest.Builder(instance).data(rainfallMapsInfo!!.past1HourUrls.values.elementAt(currentPosition)).transformations(RainfallMapImageTransformation()).build(),
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Isohyet Chart for Last Hour" else "過去一小時等雨量線圖"
                                )
                            }
                            RainfallMapMode.PAST_TWENTY_FOUR_HOUR -> {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .requiredSize(25.dp),
                                            color = Color.LightGray,
                                            strokeWidth = 3.dp,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    },
                                    model = ImageRequest.Builder(instance).data(rainfallMapsInfo!!.past24HoursUrl).transformations(RainfallMapImageTransformation()).build(),
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Isohyet Chart for Past 24-hour" else "過去二十四小時等雨量線圖"
                                )
                            }
                            RainfallMapMode.TODAY -> {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .requiredSize(25.dp),
                                            color = Color.LightGray,
                                            strokeWidth = 3.dp,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    },
                                    model = ImageRequest.Builder(instance).data(rainfallMapsInfo!!.todayUrl).transformations(RainfallMapImageTransformation()).build(),
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Isohyet Chart for Today" else "今天等雨量線圖"
                                )
                            }
                            RainfallMapMode.YESTERDAY -> {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .requiredSize(25.dp),
                                            color = Color.LightGray,
                                            strokeWidth = 3.dp,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    },
                                    model = ImageRequest.Builder(instance).data(rainfallMapsInfo!!.yesterdayUrl).transformations(RainfallMapImageTransformation()).build(),
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Isohyet Chart for Yesterday" else "昨天等雨量線圖"
                                )
                            }
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
                        text = when (currentMode) {
                            RainfallMapMode.PAST_ONE_HOUR -> {
                                val dateFormat = DateFormat.getTimeFormat(instance)
                                val timeFormat = DateTimeFormatter.ofPattern(if (dateFormat is SimpleDateFormat) dateFormat.toPattern() else "HH:mm")
                                "@".plus(rainfallMapsInfo!!.past1HourUrls.keys.elementAt(currentPosition).format(timeFormat))
                            }
                            else -> {
                                if (Registry.getInstance(instance).language == "en") currentMode.nameEn else currentMode.nameZh
                            }
                        }
                    )
                    if (!zoom) {
                        Button(
                            onClick = {
                                currentMode = currentMode.next()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(0.dp, 0.dp, 0.dp, 1.dp)
                                .width(StringUtils.scaledSize(90, instance).dp)
                                .height(StringUtils.scaledSize(30, instance).dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            content = {
                                Row (
                                    modifier = Modifier
                                        .fillMaxWidth(0.925F)
                                        .align(Alignment.Center),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        modifier = Modifier.size(StringUtils.scaledSize(13, instance).dp),
                                        imageVector = Icons.Filled.ArrowForward,
                                        contentDescription = if (Registry.getInstance(instance).language == "en") currentMode.next().nameEn else currentMode.next().nameZh,
                                        tint = Color(0xFFFFFFFF)
                                    )
                                    AutoResizeText(
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.primary,
                                        fontSizeRange = FontSizeRange(
                                            min = TextUnit(1F, TextUnitType.Sp),
                                            max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                                        ),
                                        maxLines = 1,
                                        text = if (Registry.getInstance(instance).language == "en") currentMode.next().nameEn else currentMode.next().nameZh
                                    )
                                }
                            }
                        )
                    }
                    if (currentMode == RainfallMapMode.PAST_ONE_HOUR) {
                        Button(
                            onClick = {
                                if (currentPosition > 0) {
                                    currentPosition--
                                }
                            },
                            enabled = currentPosition > 0,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(0.dp, 0.dp, 1.dp, 0.dp)
                                .width(StringUtils.scaledSize(30, instance).dp)
                                .height(StringUtils.scaledSize(30, instance).dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            content = {
                                Icon(
                                    modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp),
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Previous" else "上一頁",
                                    tint = Color(0xFFFFFFFF)
                                )
                            }
                        )
                        Button(
                            onClick = {
                                if (currentPosition < rainfallMapsInfo!!.past1HourUrls.size - 1) {
                                    currentPosition++
                                }
                            },
                            enabled = currentPosition < rainfallMapsInfo!!.past1HourUrls.size - 1,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
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
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = if (Registry.getInstance(instance).language == "en") "Next" else "下一頁",
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