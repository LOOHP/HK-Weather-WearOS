package com.loohp.hkweatherwarnings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Pair
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.compose.verticalScrollWithScrollbar
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.RemoteActivityUtils
import com.loohp.hkweatherwarnings.utils.ScreenSizeUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.UnitUtils
import com.loohp.hkweatherwarnings.utils.clamp
import com.loohp.hkweatherwarnings.utils.dp
import com.loohp.hkweatherwarnings.utils.sp
import com.loohp.hkweatherwarnings.utils.timeZone
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo
import com.loohp.hkweatherwarnings.weather.LunarDate
import com.loohp.hkweatherwarnings.weather.UVIndexType
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.ForkJoinPool


class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var today by remember { mutableStateOf(LocalDate.now(Shared.HK_TIMEZONE.toZoneId())) }
            LaunchedEffect (Unit) {
                while (true) {
                    val newNow = LocalDate.now(Shared.HK_TIMEZONE.toZoneId())
                    if (newNow != today) {
                        today = newNow
                    }
                    delay(500)
                }
            }
            MainElements(today, this)
        }
    }

}

@Composable
fun MainElements(today: LocalDate, instance: TitleActivity) {
    val weatherInfo: CurrentWeatherInfo? by remember { Shared.currentWeatherInfo.getState(instance, ForkJoinPool.commonPool()) }
    val weatherWarnings: Set<WeatherWarningsType> by remember { Shared.currentWarnings.getState(instance, ForkJoinPool.commonPool()) }
    val weatherTips: List<Pair<String, Long>> by remember { Shared.currentTips.getState(instance, ForkJoinPool.commonPool()) }
    val lunarDate: LunarDate? by remember { Shared.convertedLunarDates.getValueState(today, instance, ForkJoinPool.commonPool()) }

    HKWeatherTheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(false) }
        LaunchedEffect (scrollInProgress) {
            if (scrollInProgress) {
                scrollCounter++
            }
        }
        LaunchedEffect (scrollCounter, scrollReachedEnd) {
            delay(50)
            if (scrollReachedEnd && scrollMoved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            scrollMoved = true
        }
        LaunchedEffect (Unit) {
            focusRequester.requestFocus()
        }

        Column (
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollWithScrollbar(
                    state = scroll,
                    flingBehavior = ScrollableDefaults.flingBehavior()
                )
                .onRotaryScrollEvent {
                    scope.launch {
                        scroll.animateScrollBy(
                            it.verticalScrollPixels * 1.5F,
                            TweenSpec(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    }
                    true
                }
                .focusRequester(
                    focusRequester = focusRequester
                )
                .focusable()
        ) {
            WeatherInfoElements(weatherInfo, weatherWarnings, weatherTips, lunarDate, instance)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp, 0.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UsageText(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                OpenHKOAppButton(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                ChangeLocationButton(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                SetRefreshRateButton(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Row (
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageButton(instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                    UpdateTilesButton(instance)
                }
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                CreditVersionText(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(14, instance).dp))
            }
        }
    }
}

@Composable
fun UpdatingElements(instance: TitleActivity) {
    Box(
        modifier = Modifier
            .width(
                UnitUtils.pixelsToDp(
                    instance,
                    ScreenSizeUtils
                        .getScreenWidth(instance)
                        .toFloat()
                ).dp
            )
            .height(
                UnitUtils.pixelsToDp(
                    instance,
                    ScreenSizeUtils
                        .getScreenHeight(instance)
                        .toFloat()
                ).dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(0.8F),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp),
            text = if (Registry.getInstance(instance).language == "en") "Updating weather information..." else "正在更新天氣資訊..."
        )
    }
}

@Composable
fun WeatherInfoElements(weatherInfo: CurrentWeatherInfo?, weatherWarnings: Set<WeatherWarningsType>, weatherTips: List<Pair<String, Long>>, lunarDate: LunarDate?, instance: TitleActivity) {
    if (weatherInfo == null) {
        UpdatingElements(instance)
    } else {
        val systemTimeFormat = DateFormat.getTimeFormat(instance)
        val timeFormat = DateTimeFormatter.ofPattern(if (systemTimeFormat is SimpleDateFormat) systemTimeFormat.toPattern() else "HH:mm")
        Column (
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(
                        UnitUtils.pixelsToDp(
                            instance,
                            ScreenSizeUtils
                                .getScreenWidth(instance)
                                .toFloat()
                        ).dp
                    )
                    .height(
                        UnitUtils.pixelsToDp(
                            instance,
                            ScreenSizeUtils
                                .getScreenHeight(instance)
                                .toFloat()
                        ).dp
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 25.dp, 0.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AutoResizeText(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontSizeRange = FontSizeRange(
                                min = TextUnit(1F, TextUnitType.Sp),
                                max = StringUtils.scaledSize(17F, instance).sp.clamp(max = 18.dp)
                            ),
                            text = weatherInfo.weatherStation
                        )
                        if (Registry.getInstance(instance).location.first == "GPS") {
                            Image(
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 4.dp, 0.dp)
                                    .size(StringUtils.scaledSize(16F, instance).dp),
                                painter = painterResource(R.mipmap.gps),
                                contentDescription = if (Registry.getInstance(instance).language == "en") "GPS Location" else "你的位置"
                            )
                        }
                    }
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val lastUpdateText = (if (Registry.getInstance(instance).language == "en") "Updated: " else "更新時間: ").plus(
                            DateFormat.getTimeFormat(instance).timeZone(Shared.HK_TIMEZONE).format(Date(Shared.currentWeatherInfo.getLastSuccessfulUpdateTime())))
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(11F.dp.sp.value, TextUnitType.Sp),
                            text = lastUpdateText
                        )
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 0.dp)
                                .size(11.dp)
                                .clickable {
                                    Shared.currentWeatherInfo.reset()
                                    Shared.currentWarnings.reset()
                                    Shared.currentTips.reset()
                                    Shared.currentWeatherInfo.getLatestValue(
                                        instance,
                                        ForkJoinPool.commonPool(),
                                        true
                                    )
                                    Shared.currentWarnings.getLatestValue(
                                        instance,
                                        ForkJoinPool.commonPool(),
                                        true
                                    )
                                    Shared.currentTips.getLatestValue(
                                        instance,
                                        ForkJoinPool.commonPool(),
                                        true
                                    )
                                },
                            painter = painterResource(R.mipmap.reload),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Reload" else "重新載入"
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(5.dp, 5.dp)
                            .size(StringUtils.scaledSize(55, instance).dp),
                        painter = painterResource(weatherInfo.weatherIcon.iconId),
                        contentDescription = weatherInfo.weatherIcon.iconName
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(35F, TextUnitType.Sp),
                        text = String.format("%.1f", weatherInfo.currentTemperature).plus("°")
                    )
                }
                Column (
                    modifier = Modifier
                        .padding(0.dp, StringUtils.scaledSize(100, instance).dp, 0.dp, 0.dp)
                        .align(Alignment.Center)
                ) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 3.dp)
                                .size(StringUtils.scaledSize(14, instance).dp),
                            painter = painterResource(R.mipmap.highest),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Highest Temperature" else "最高氣溫"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(13F, TextUnitType.Sp),
                            text = String.format("%.1f", weatherInfo.highestTemperature).plus("° ")
                        )
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 3.dp)
                                .size(StringUtils.scaledSize(14, instance).dp),
                            painter = painterResource(R.mipmap.lowest),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Lowest Temperature" else "最低氣溫"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(13F, TextUnitType.Sp),
                            text = String.format("%.1f", weatherInfo.lowestTemperature).plus("°")
                        )
                    }
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 3.dp)
                                .size(StringUtils.scaledSize(14, instance).dp),
                            painter = painterResource(R.mipmap.umbrella),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Chance of Rain" else "降雨概率"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(13F, TextUnitType.Sp),
                            text = (if (Registry.getInstance(instance).language == "en") "Chance of Rain" else "降雨概率")
                                .plus(String.format(" %.0f", weatherInfo.chanceOfRain).plus("%"))
                        )
                    }
                }
            }
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val today = LocalDate.now(Shared.HK_TIMEZONE.toZoneId())
                if (Registry.getInstance(instance).language == "en") {
                    Box (
                        modifier = Modifier.fillMaxWidth(0.9F),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizeText(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSizeRange = FontSizeRange(
                                min = TextUnit(1F, TextUnitType.Sp),
                                max = TextUnit(13F, TextUnitType.Sp),
                            ),
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            text = today.format(
                                DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy (EEEE)",
                                    Locale.ENGLISH
                                )
                            )
                        )
                    }
                } else {
                    Box (
                        modifier = Modifier.fillMaxWidth(0.9F),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizeText(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSizeRange = FontSizeRange(
                                min = TextUnit(1F, TextUnitType.Sp),
                                max = TextUnit(13F, TextUnitType.Sp),
                            ),
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            text = today.format(
                                DateTimeFormatter.ofPattern(
                                    "yyyy年 MM月 dd日 (EEEE)",
                                    Locale.TRADITIONAL_CHINESE
                                )
                            )
                        )
                    }
                    if (lunarDate != null) {
                        Spacer(modifier = Modifier.size(2.dp))
                        Box (
                            modifier = Modifier.fillMaxWidth(0.9F),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoResizeText(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontSizeRange = FontSizeRange(
                                    min = TextUnit(1F, TextUnitType.Sp),
                                    max = TextUnit(13F, TextUnitType.Sp),
                                ),
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                text = lunarDate.toString()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(2.dp))
                val formatter = DateFormat.getTimeFormat(instance).timeZone(Shared.HK_TIMEZONE)
                var timeText by remember { mutableStateOf(formatter.format(Date())) }
                LaunchedEffect (Unit) {
                    while (true) {
                        timeText = formatter.format(Date())
                        delay(500)
                    }
                }
                Box {
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = timeText
                    )
                }
                if (Registry.getInstance(instance).language != "en" && lunarDate != null && lunarDate.hasClimatology()) {
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = "節氣: ".plus(lunarDate.climatology)
                    )
                }
                Spacer(modifier = Modifier.size(25.dp))
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(13F, TextUnitType.Sp),
                    text = if (Registry.getInstance(instance).language == "en") "Weather Warnings" else "天氣警告"
                )
                Spacer(modifier = Modifier.size(4.dp))
                if (weatherWarnings.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(20.dp, 0.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(10F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "There are currently no active warning signals." else "目前沒有任何天氣警告信號"
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                } else {
                    val list = weatherWarnings.toList()
                    for (i in list.indices step 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (u in i until (i + 3).coerceAtMost(list.size)) {
                                val warning = list[u]
                                Image(
                                    modifier = Modifier
                                        .padding(3.dp, 3.dp)
                                        .size(StringUtils.scaledSize(40, instance).dp),
                                    painter = painterResource(warning.iconId),
                                    contentDescription = warning.iconName
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(13F, TextUnitType.Sp),
                    text = if (Registry.getInstance(instance).language == "en") "Special Weather Tips" else "特別天氣提示"
                )
                Spacer(modifier = Modifier.size(4.dp))
                if (weatherTips.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(20.dp, 0.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(10F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "There are currently no active special weather tips." else "目前沒有任何特別天氣提示"
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                } else {
                    for (tip in weatherTips) {
                        Text(
                            modifier = Modifier.padding(20.dp, 0.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(15F, TextUnitType.Sp),
                            text = tip.first
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        val date = Date(tip.second)
                        val lastUpdateText =
                            DateFormat.getDateFormat(instance).timeZone(Shared.HK_TIMEZONE)
                                .format(date)
                                .plus(" ")
                                .plus(
                                    DateFormat.getTimeFormat(instance).timeZone(Shared.HK_TIMEZONE)
                                        .format(date)
                                )
                        Text(
                            modifier = Modifier.padding(5.dp, 0.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(10F, TextUnitType.Sp),
                            text = lastUpdateText
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.uvindex),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "UV Index" else "紫外線指數"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "UV Index" else "紫外線指數"
                    )
                }
                if (weatherInfo.uvIndex >= 0) {
                    val uvIndexType = UVIndexType.getByValue(weatherInfo.uvIndex)
                    Text(
                        textAlign = TextAlign.Center,
                        color = Color(uvIndexType.color),
                        fontSize = TextUnit(25F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = String.format("%.0f", weatherInfo.uvIndex).plus(" ")
                            .plus(if (Registry.getInstance(instance).language == "en") uvIndexType.en else uvIndexType.zh)
                    )
                } else {
                    Text(
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFF9625),
                        fontSize = TextUnit(25F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = "-"
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.humidity),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Relative Humidity" else "相對濕度"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Relative Humidity" else "相對濕度"
                    )
                }
                Text(
                    textAlign = TextAlign.Center,
                    color = Color(0xFF3CB0FF),
                    fontSize = TextUnit(25F, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    text = String.format("%.0f", weatherInfo.currentHumidity).plus("%")
                )
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.wind),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Wind" else "風向風速"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Wind" else "風向風速"
                    )
                }
                val windText = if (weatherInfo.windSpeed < 0F) {
                    "-"
                } else {
                    weatherInfo.windDirection.plus(" ")
                        .plus(String.format("%.0f", weatherInfo.windSpeed)).plus(if (Registry.getInstance(instance).language == "en") " km/h" else "公里/小時")
                }
                Box (
                    modifier = Modifier.fillMaxWidth(0.9F),
                    contentAlignment = Alignment.Center
                ) {
                    AutoResizeText(
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFFFFFF),
                        fontSizeRange = FontSizeRange(
                            min = TextUnit(1F, TextUnitType.Sp),
                            max = TextUnit(25F, TextUnitType.Sp)
                        ),
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        text = windText
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.wind),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Gust" else "陣風",
                        colorFilter = ColorFilter.tint(Color(0xFF26D4FF))
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Gust" else "陣風"
                    )
                }
                val gustText = if (weatherInfo.windSpeed < 0F) {
                    "-"
                } else {
                    String.format("%.0f", weatherInfo.gust).plus(if (Registry.getInstance(instance).language == "en") " km/h" else "公里/小時")
                }
                Box (
                    modifier = Modifier.fillMaxWidth(0.9F),
                    contentAlignment = Alignment.Center
                ) {
                    AutoResizeText(
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFFFFFF),
                        fontSizeRange = FontSizeRange(
                            min = TextUnit(1F, TextUnitType.Sp),
                            max = TextUnit(25F, TextUnitType.Sp)
                        ),
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        text = gustText
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.sunrise),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Sunrise" else "日出"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Sunrise" else "日出"
                    )
                }
                Text(
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFC32B),
                    fontSize = TextUnit(25F, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    text = weatherInfo.sunriseTime.format(timeFormat)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.sunset),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Sunset" else "日落"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Sunset" else "日落"
                    )
                }
                Text(
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFF802B),
                    fontSize = TextUnit(25F, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    text = weatherInfo.sunsetTime.format(timeFormat)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(14, instance).dp),
                        painter = painterResource(R.mipmap.moon),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Moon Phase" else "月相"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(13F, TextUnitType.Sp),
                        text = if (Registry.getInstance(instance).language == "en") "Moon Phase" else "月相"
                    )
                }
                AsyncImage(
                    modifier = Modifier.padding(8.sp.dp).size(25.sp.dp),
                    model = "https://pda.weather.gov.hk/locspc/android_data/img/moonphase.jpg?t=".plus(Shared.currentWeatherInfo.getLastSuccessfulUpdateTime()),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Moon Phase" else "月相"
                )
                Spacer(modifier = Modifier.size(10.dp))
                val moonrise = @Composable {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 3.dp)
                                .size(StringUtils.scaledSize(14, instance).dp),
                            painter = painterResource(R.mipmap.moonrise),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Moonrise" else "月出"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(13F, TextUnitType.Sp),
                            text = if (Registry.getInstance(instance).language == "en") "Moonrise" else "月出"
                        )
                    }
                    Text(
                        textAlign = TextAlign.Center,
                        color = Color(0xFFDFDFDF),
                        fontSize = TextUnit(25F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = if (weatherInfo.moonriseTime == null) "-" else weatherInfo.moonriseTime.format(timeFormat)
                    )
                }
                val moonset = @Composable {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 3.dp)
                                .size(StringUtils.scaledSize(14, instance).dp),
                            painter = painterResource(R.mipmap.moonset),
                            contentDescription = if (Registry.getInstance(instance).language == "en") "Moonset" else "月落"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(13F, TextUnitType.Sp),
                            text = if (Registry.getInstance(instance).language == "en") "Moonset" else "月落"
                        )
                    }
                    Text(
                        textAlign = TextAlign.Center,
                        color = Color(0xFFBEBEBE),
                        fontSize = TextUnit(25F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        text = if (weatherInfo.moonsetTime == null) "-" else weatherInfo.moonsetTime.format(timeFormat)
                    )
                }
                if (weatherInfo.moonriseTime == null || weatherInfo.moonsetTime == null || weatherInfo.moonriseTime.isBefore(weatherInfo.moonsetTime)) {
                    moonrise()
                    Spacer(modifier = Modifier.size(10.dp))
                    moonset()
                } else {
                    moonset()
                    Spacer(modifier = Modifier.size(10.dp))
                    moonrise()
                }
                Spacer(modifier = Modifier.size(10.dp))
                Button(
                    onClick = {
                        instance.startActivity(Intent(instance, RadarActivity::class.java))
                    },
                    modifier = Modifier
                        .width(StringUtils.scaledSize(180, instance).dp)
                        .height(StringUtils.scaledSize(45, instance).dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        AutoResizeText(
                            modifier = Modifier
                                .fillMaxWidth(0.9F)
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSizeRange = FontSizeRange(
                                min = TextUnit(1F, TextUnitType.Sp),
                                max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                            ),
                            text = if (Registry.getInstance(instance).language == "en") "Radar Image (64 km)" else "雷達圖像 (64 公里)"
                        )
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
                Spacer(
                    modifier = Modifier
                        .padding(20.dp, 0.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF333333))
                )
                Spacer(modifier = Modifier.size(20.dp))
                HourlyElements(weatherInfo, timeFormat, instance)
                Spacer(modifier = Modifier.size(20.dp))
                Spacer(
                    modifier = Modifier
                        .padding(20.dp, 0.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF333333))
                )
                Spacer(modifier = Modifier.size(20.dp))
                ForecastElements(weatherInfo, instance)
                Spacer(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF333333)))
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun HourlyElements(weatherInfo: CurrentWeatherInfo, timeFormat: DateTimeFormatter, instance: TitleActivity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var count = 0
        val nowHour = LocalDateTime.now(Shared.HK_TIMEZONE.toZoneId())
        for (hourInfo in weatherInfo.hourlyWeatherInfo) {
            if (hourInfo.time.isBefore(nowHour)) {
                continue
            }
            if (++count > 12) {
                break
            }
            Spacer(modifier = Modifier.size(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val small = ScreenSizeUtils.getScreenWidth(instance) < 450
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 14.dp),
                    fontWeight = FontWeight.Bold,
                    text = hourInfo.time.format(timeFormat).plus(if (small) " " else "     ")
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val size = if (small) 10 else 15
                    Image(
                        modifier = Modifier
                            .padding(1.dp, 1.dp)
                            .size(size.dp),
                        painter = painterResource(R.mipmap.humidity),
                        contentDescription = if (Registry.getInstance(instance).language == "en") "Relative Humidity" else "相對濕度"
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = TextUnit(size - 1F, TextUnitType.Sp).clamp(max = (size - 1).dp),
                        text = String.format("%.0f", hourInfo.humidity).plus("%").plus(if (small) " " else "  ")
                    )
                }
                Image(
                    modifier = Modifier
                        .padding(1.dp, 1.dp)
                        .size(20.dp),
                    painter = painterResource(hourInfo.weatherIcon.iconId),
                    contentDescription = hourInfo.weatherIcon.iconName
                )
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 14.dp),
                    fontWeight = FontWeight.Bold,
                    text = String.format("%.1f", hourInfo.temperature).plus("°")
                )
            }
            Spacer(modifier = Modifier.size(5.dp))
        }
    }
}

@Composable
fun ForecastElements(weatherInfo: CurrentWeatherInfo, instance: TitleActivity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (dayInfo in weatherInfo.forecastInfo) {
            Spacer(modifier = Modifier.size(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 14.dp),
                    fontWeight = FontWeight.Bold,
                    text = dayInfo.dayOfWeek.getDisplayName(
                        TextStyle.SHORT,
                        if (Registry.getInstance(instance).language == "en") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE
                    )
                )
                Image(
                    modifier = Modifier
                        .padding(1.dp, 1.dp)
                        .size(10.dp),
                    painter = painterResource(R.mipmap.humidity),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Relative Humidity" else "相對濕度"
                )
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(9F, TextUnitType.Sp).clamp(max = 9.dp),
                    text = String.format(
                        "%.0f",
                        (dayInfo.minRelativeHumidity + dayInfo.maxRelativeHumidity) / 2F
                    ).plus("%")
                )
                Image(
                    modifier = Modifier
                        .padding(1.dp, 1.dp)
                        .size(10.dp),
                    painter = painterResource(R.mipmap.umbrella),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Chance of Rain" else "降雨概率"
                )
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(9F, TextUnitType.Sp).clamp(max = 9.dp),
                    text = if (dayInfo.chanceOfRain >= 0) String.format(
                        "%.0f",
                        dayInfo.chanceOfRain
                    ).plus("%") else "??%"
                )
                Image(
                    modifier = Modifier
                        .padding(1.dp, 1.dp)
                        .size(20.dp),
                    painter = painterResource(dayInfo.weatherIcon.iconId),
                    contentDescription = dayInfo.weatherIcon.iconName
                )
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 14.dp),
                    fontWeight = FontWeight.Bold,
                    text = String.format("%.0f", dayInfo.lowestTemperature).plus("-")
                        .plus(String.format("%.0f", dayInfo.highestTemperature))
                        .plus("°")
                )
            }
            Spacer(modifier = Modifier.size(5.dp))
        }
    }
}

@Composable
fun OpenHKOAppButton(instance: TitleActivity) {
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("myobservatory:"))
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Unable to connect to phone" else "無法連接到手機", Toast.LENGTH_SHORT).show()
                    }
                },
                failed = {
                    val playIntent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=hko.MyObservatory_v1_0"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = playIntent,
                        failed = {
                            instance.runOnUiThread {
                                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Failed to connect to phone" else "連接手機失敗", Toast.LENGTH_SHORT).show()
                            }
                        },
                        success = {
                            instance.runOnUiThread {
                                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                success = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp),
                text = if (Registry.getInstance(instance).language == "en") "Open MyObservatory" else "開啟我的天文台"
            )
        }
    )
}

@Composable
fun ChangeLocationButton(instance: TitleActivity) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, ChangeLocationActivity::class.java))
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = 16.dp),
                text = if (Registry.getInstance(instance).language == "en") "Set Weather Location" else "設定天氣資訊位置"
            )
        }
    )
}

@Composable
fun SetRefreshRateButton(instance: TitleActivity) {
    var refreshRate by remember { mutableStateOf(Shared.REFRESH_INTERVAL.invoke(instance)) }
    Button(
        onClick = {
            if (refreshRate <= 900000) {
                Registry.getInstance(instance).setRefreshRate(1800000, instance)
            } else if (refreshRate <= 1800000) {
                Registry.getInstance(instance).setRefreshRate(3600000, instance)
            } else if (refreshRate <= 3600000) {
                Registry.getInstance(instance).setRefreshRate(Long.MAX_VALUE, instance)
            } else {
                Registry.getInstance(instance).setRefreshRate(900000, instance)
            }
            refreshRate = Shared.REFRESH_INTERVAL.invoke(instance)
            Shared.startBackgroundService(instance)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            val timeText = if (refreshRate >= Shared.NEVER_REFRESH_INTERVAL) {
                if (Registry.getInstance(instance).language == "en") "Manual" else "手動更新"
            } else {
                (refreshRate / 60000).toString().plus(if (Registry.getInstance(instance).language == "en") " Mins" else "分鐘")
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = 16.dp),
                text = (if (Registry.getInstance(instance).language == "en") "Set Refresh Rate: " else "更新頻率: ").plus(timeText)
            )
        }
    )
}

@Composable
fun LanguageButton(instance: TitleActivity) {
    Button(
        onClick = {
            Registry.getInstance(instance).setLanguage(if (Registry.getInstance(instance).language == "en") "zh" else "en", instance)
            Shared.currentWeatherInfo.reset()
            Shared.currentWarnings.reset()
            Shared.currentTips.reset()
            instance.startActivity(Intent(instance, TitleActivity::class.java))
            instance.finishAffinity()
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(90, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = 16.dp),
                text = if (Registry.getInstance(instance).language == "en") "中文" else "English"
            )
        }
    )
}

@Composable
fun UpdateTilesButton(instance: TitleActivity) {
    Button(
        onClick = {
            Shared.currentWeatherInfo.reset()
            Shared.currentWarnings.reset()
            Shared.currentTips.reset()
            Registry.getInstance(instance).updateTileServices(instance)
            instance.runOnUiThread {
                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Refreshing all tiles" else "正在更新所有資訊方塊", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(35, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color(0xFFFFFFFF)
        ),
        content = {
            Image(
                modifier = Modifier.size(StringUtils.scaledSize(19, instance).dp),
                painter = painterResource(R.mipmap.reload),
                contentDescription = if (Registry.getInstance(instance).language == "en") "Refresh all tiles" else "更新所有資訊方塊"
            )
        }
    )
}

@Composable
fun UsageText(instance: TitleActivity) {
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = TextUnit(1F, TextUnitType.Sp),
            max = TextUnit(15F, TextUnitType.Sp)
        ),
        text = if (Registry.getInstance(instance).language == "en") "Add tiles to view weather info!" else "添加資訊方塊查看天氣資訊"
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreditVersionText(instance: TitleActivity) {
    val packageInfo = instance.packageManager.getPackageInfo(instance.packageName, 0)
    val haptic = LocalHapticFeedback.current
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.loohp.hkweatherwarnings"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://loohpjames.com"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                }
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(1.5F, instance), TextUnitType.Em),
        text = instance.resources.getString(R.string.app_name).plus(" v").plus(packageInfo.versionName).plus(" (").plus(packageInfo.longVersionCode).plus(")\n@LoohpJames")
    )
}