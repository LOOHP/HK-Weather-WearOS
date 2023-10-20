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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Pair
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
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
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.compose.fadeIn
import com.loohp.hkweatherwarnings.compose.fullPageVerticalLazyScrollbar
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
import com.loohp.hkweatherwarnings.weather.HeatStressAtWorkWarningAction
import com.loohp.hkweatherwarnings.weather.LocalForecastInfo
import com.loohp.hkweatherwarnings.weather.LunarDate
import com.loohp.hkweatherwarnings.weather.SpecialTyphoonInfo
import com.loohp.hkweatherwarnings.weather.UVIndexType
import com.loohp.hkweatherwarnings.weather.WeatherStatusIcon
import com.loohp.hkweatherwarnings.weather.WeatherWarningsCategory
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.ForkJoinPool


enum class Section {
    MAIN, DATE, WARNINGS, HEAT_STRESS_AT_WORK_WARNING, TIPS, HUMIDITY, UVINDEX, WIND, SUN, MOON, FORECAST, HOURLY;
}

class ItemList : ArrayList<kotlin.Pair<@Composable () -> Unit, Section?>>() {

    fun add(itemProvider: @Composable () -> Unit): Boolean {
        return add(itemProvider to null)
    }

    fun add(section: Section, itemProvider: @Composable () -> Unit): Boolean {
        return add(itemProvider to section)
    }

    fun addAll(section: Section, itemProviders: Collection<@Composable () -> Unit>): Boolean {
        return addAll(itemProviders.map { it to section })
    }

    fun getSectionIndex(section: Section): Int {
        return indexOfFirst { section == it.second }
    }

}

class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCreate(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCreate(intent)
    }

    private fun handleCreate(intent: Intent) {
        var launchSection: Section? = null
        if (intent.extras != null && intent.extras!!.containsKey("launchSection")) {
            try {
                launchSection = Section.valueOf(intent.extras!!.getString("launchSection")!!.uppercase())
            } catch (_: Exception) { }
        }

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
            MainElements(today, launchSection, this)
        }
    }

}

@Composable
fun MainElements(today: LocalDate, launchSection: Section? = null, instance: TitleActivity) {
    val weatherInfo by remember { Shared.currentWeatherInfo.getState(instance) }
    val weatherWarnings by remember { Shared.currentWarnings.getState(instance) }
    val weatherTips by remember { Shared.currentTips.getState(instance) }
    val lunarDate by remember { Shared.convertedLunarDates.getValueState(today) }

    val weatherInfoUpdating by remember { Shared.currentWeatherInfo.getCurrentlyUpdatingState(instance) }
    val weatherWarningsUpdating by remember { Shared.currentWarnings.getCurrentlyUpdatingState(instance) }
    val weatherTipsUpdating by remember { Shared.currentTips.getCurrentlyUpdatingState(instance) }
    val combinedUpdating by remember { derivedStateOf { weatherInfoUpdating || weatherWarningsUpdating || weatherTipsUpdating } }

    val weatherInfoUpdateSuccessful by remember { Shared.currentWeatherInfo.getLastUpdateSuccessState(instance) }
    val weatherWarningsUpdateSuccessful by remember { Shared.currentWarnings.getLastUpdateSuccessState(instance) }
    val weatherTipsUpdateSuccessful by remember { Shared.currentTips.getLastUpdateSuccessState(instance) }
    val updateSuccessful by remember { derivedStateOf { weatherInfoUpdateSuccessful && weatherWarningsUpdateSuccessful && weatherTipsUpdateSuccessful } }

    LaunchedEffect (Unit) {
        Shared.currentWeatherInfo.getLatestValue(instance, ForkJoinPool.commonPool())
        Shared.currentWarnings.getLatestValue(instance, ForkJoinPool.commonPool())
        Shared.currentTips.getLatestValue(instance, ForkJoinPool.commonPool())
        Shared.convertedLunarDates.getValue(today, instance, ForkJoinPool.commonPool())
        Registry.getInstance(instance).updateTileServices(instance)
    }

    HKWeatherTheme {
        var moonPhaseUrl by remember { mutableStateOf("") }
        val elements by remember { derivedStateOf { generateWeatherInfoItems(weatherInfoUpdating, combinedUpdating, updateSuccessful, weatherInfo, weatherWarnings, weatherTips, lunarDate, moonPhaseUrl, instance) } }

        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(0) }

        val mutex by remember { mutableStateOf(Mutex()) }
        val animatedScrollValue = remember { Animatable(0F) }
        var previousScrollValue by remember { mutableStateOf(0F) }
        LaunchedEffect (animatedScrollValue.value) {
            val diff = previousScrollValue - animatedScrollValue.value
            scroll.scrollBy(diff)
            previousScrollValue -= diff
        }

        LaunchedEffect (scrollInProgress) {
            if (scrollInProgress) {
                scrollCounter++
            }
        }
        LaunchedEffect (scrollCounter, scrollReachedEnd) {
            delay(50)
            if (scrollReachedEnd && scrollMoved > 1) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (scrollMoved <= 1) {
                scrollMoved++
            }
        }
        LaunchedEffect (weatherInfo) {
            val url = "https://pda.weather.gov.hk/locspc/android_data/img/moonphase.jpg?t=".plus(Shared.currentWeatherInfo.getLastSuccessfulUpdateTime(instance))
            moonPhaseUrl = url
            instance.imageLoader.execute(ImageRequest.Builder(instance).data(url).build())
        }
        LaunchedEffect (Unit) {
            focusRequester.requestFocus()
            launchSection?.let {
                val index = elements.getSectionIndex(it)
                if (index >= 0) {
                    scroll.animateScrollToItem(index, -ScreenSizeUtils.getScreenHeight(instance) / 5)
                }
            }
        }

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll
                )
                .onRotaryScrollEvent {
                    scope.launch {
                        mutex.withLock {
                            val target = it.verticalScrollPixels + animatedScrollValue.value
                            animatedScrollValue.snapTo(target)
                            previousScrollValue = target
                        }
                        animatedScrollValue.animateTo(
                            0F,
                            TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing)
                        )
                    }
                    true
                }
                .focusRequester(
                    focusRequester = focusRequester
                )
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            for ((it, _) in elements) {
                item {
                    it.invoke()
                }
            }
            item {
                UsageText(instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            item {
                OpenHKOAppButton(instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            item {
                ChangeLocationButton(instance, !combinedUpdating)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            item {
                SetRefreshRateButton(instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            item {
                Row (
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageButton(instance, !combinedUpdating)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                    UpdateTilesButton(combinedUpdating, instance)
                }
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            item {
                CreditVersionText(instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(14, instance).dp))
            }
        }
    }
}

@Composable
fun UpdatingElements(instance: TitleActivity) {
    val currentProgress by remember { Shared.currentWeatherInfo.getCurrentProgressState(instance) }
    val progressAnimation by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "LoadingProgressAnimation"
    )

    Box(
        modifier = Modifier
            .fadeIn()
            .width(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenWidth(instance).toFloat()).dp)
            .height(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenHeight(instance).toFloat()).dp),
        contentAlignment = Alignment.Center
    ) {
        Column (
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(0.8F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = StringUtils.scaledSize(16F, instance).sp,
                text = if (Registry.getInstance(instance).language == "en") "Updating weather information..." else "正在更新天氣資訊..."
            )
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(15, instance).dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7F)
                    .padding(25.dp, 0.dp),
                color = Color(0xFFF9DE09),
                trackColor = Color(0xFF797979),
                progress = progressAnimation
            )
        }
    }
}

@Composable
fun UpdateFailedElements(instance: TitleActivity) {
    Column(
        modifier = Modifier
            .fadeIn()
            .width(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenWidth(instance).toFloat()).dp)
            .height(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenHeight(instance).toFloat()).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(0.8F),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = StringUtils.scaledSize(16F, instance).sp,
            text = if (Registry.getInstance(instance).language == "en") "Unable to update weather information" else "無法更新天氣資訊"
        )
        Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
        Button(
            onClick = {
                Shared.currentWeatherInfo.reset(instance)
                Shared.currentWarnings.reset(instance)
                Shared.currentTips.reset(instance)
                Shared.currentWeatherInfo.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                Shared.currentWarnings.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                Shared.currentTips.getLatestValue(instance, ForkJoinPool.commonPool(), true)
            },
            modifier = Modifier
                .width(StringUtils.scaledSize(90, instance).dp)
                .height(StringUtils.scaledSize(40, instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFD3A225),
                contentColor = MaterialTheme.colors.primary
            ),
            content = {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.9F)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 13F.sp.clamp(max = 14.dp),
                    text = if (Registry.getInstance(instance).language == "en") "Retry" else "重試"
                )
            }
        )
    }
}

@Composable
fun DateTimeElements(lunarDate: LunarDate?, instance: TitleActivity) {
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
                    min = 1F.sp,
                    max = 13F.sp,
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
                    min = 1F.sp,
                    max = 13F.sp,
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
                        min = 1F.sp,
                        max = 13F.sp,
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
            fontSize = 13F.sp,
            fontWeight = FontWeight.Bold,
            text = timeText
        )
    }
    if (Registry.getInstance(instance).language != "en" && lunarDate != null && lunarDate.hasClimatology()) {
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 13F.sp,
            fontWeight = FontWeight.Bold,
            text = "節氣: ".plus(lunarDate.climatology)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun generateWeatherInfoItems(updating: Boolean, combinedUpdating: Boolean, lastUpdateSuccessful: Boolean, weatherInfo: CurrentWeatherInfo?, weatherWarnings: Map<WeatherWarningsType, String?>, weatherTips: List<Pair<String, Long>>, lunarDate: LunarDate?, moonPhaseUrl: String, instance: TitleActivity): ItemList {
    val itemList = ItemList()
    if (weatherInfo == null) {
        if (updating) {
            itemList.add { UpdatingElements(instance) }
        } else {
            itemList.add { UpdateFailedElements(instance) }
        }
    } else {
        val systemTimeFormat = DateFormat.getTimeFormat(instance)
        val timeFormat = DateTimeFormatter.ofPattern(if (systemTimeFormat is SimpleDateFormat) systemTimeFormat.toPattern() else "HH:mm")
        itemList.add(Section.MAIN) {
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .fadeIn()
                    .width(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenWidth(instance).toFloat()).dp)
                    .height(UnitUtils.pixelsToDp(instance, ScreenSizeUtils.getScreenHeight(instance).toFloat()).dp),
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
                            .fillMaxWidth(0.65F)
                            .padding(0.dp, 25.dp, 0.dp, 0.dp)
                            .combinedClickable(
                                onClick = {
                                    instance.startActivity(Intent(instance, ChangeLocationActivity::class.java))
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        var dynamicFontSize by remember { mutableStateOf({ StringUtils.scaledSize(16F, instance) }) }
                        var dynamicReady by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1F, false)
                                .requiredHeight(StringUtils.scaledSize(30F, instance).dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            AutoResizeText(
                                onFontSizeChange = { size, readyToDraw ->
                                    dynamicFontSize = { UnitUtils.spToDp(instance, size.value) }
                                    dynamicReady = readyToDraw
                                },
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSizeRange = FontSizeRange(
                                    min = 1F.sp,
                                    max = StringUtils.scaledSize(17F, instance).sp.clamp(max = 18.dp)
                                ),
                                text = weatherInfo.weatherStation
                            )
                        }
                        if (Registry.getInstance(instance).location.first == "GPS") {
                            Image(
                                modifier = Modifier
                                    .padding((dynamicFontSize.invoke() / 8F).dp, 0.dp, 0.dp, 0.dp)
                                    .size(dynamicFontSize.invoke().dp)
                                    .align(Alignment.Bottom)
                                    .alpha(if (dynamicReady) 1F else 0F)
                                    .offset(0.dp, (dynamicFontSize.invoke() / -4F).dp),
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
                        var lastUpdateText = (if (Registry.getInstance(instance).language == "en") "Updated: " else "更新時間: ").plus(
                            DateFormat.getTimeFormat(instance).timeZone(Shared.HK_TIMEZONE).format(Date(Shared.currentWeatherInfo.getLastSuccessfulUpdateTime(instance))))
                        if (!lastUpdateSuccessful && !combinedUpdating) {
                            lastUpdateText = lastUpdateText.plus(if (Registry.getInstance(instance).language == "en") " (Update Failed)" else " (無法更新)")
                        }
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = 11F.dp.sp.value.sp,
                            text = lastUpdateText
                        )
                        Image(
                            modifier = Modifier
                                .padding(3.dp, 0.dp)
                                .size(11.dp)
                                .combinedClickable(
                                    onClick = {
                                        Shared.currentWeatherInfo.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                        Shared.currentWarnings.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                        Shared.currentTips.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Shared.currentWeatherInfo.reset(instance)
                                        Shared.currentWarnings.reset(instance)
                                        Shared.currentTips.reset(instance)
                                        Shared.currentWeatherInfo.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                        Shared.currentWarnings.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                        Shared.currentTips.getLatestValue(instance, ForkJoinPool.commonPool(), true)
                                    }
                                ),
                            painter = painterResource(if (updating) R.mipmap.reloading else R.mipmap.reload),
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
                    var weatherDescription = if (Registry.getInstance(instance).language == "en") weatherInfo.weatherIcon.descriptionEn else weatherInfo.weatherIcon.descriptionZh
                    if (weatherInfo.nextWeatherIcon != null) {
                        weatherDescription = weatherDescription.plus(if (Registry.getInstance(instance).language == "en") " to ".plus(weatherInfo.nextWeatherIcon.descriptionEn) else " 至 ".plus(weatherInfo.nextWeatherIcon.descriptionZh))
                    }
                    Box (
                        modifier = Modifier
                            .padding(5.dp, 5.dp, 5.dp, 5.dp)
                            .combinedClickable(
                                onClick = {
                                    instance.runOnUiThread {
                                        Toast
                                            .makeText(
                                                instance,
                                                weatherDescription,
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            modifier = Modifier.size(StringUtils.scaledSize(55, instance).dp),
                            painter = painterResource(weatherInfo.weatherIcon.iconId),
                            contentDescription = weatherDescription
                        )
                        if (weatherInfo.nextWeatherIcon != null) {
                            Image(
                                modifier = Modifier
                                    .size(StringUtils.scaledSize(20, instance).dp)
                                    .offset(5.dp, 0.dp)
                                    .background(
                                        Color(0xFF000000),
                                        RoundedCornerShape(StringUtils.scaledSize(3F, instance).dp)
                                    )
                                    .align(Alignment.BottomEnd),
                                painter = painterResource(weatherInfo.nextWeatherIcon.iconId),
                                contentDescription = weatherDescription
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    openHKOApp(instance)
                                }
                            ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = StringUtils.scaledSize(35, instance).dp.sp,
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
                            fontSize = StringUtils.scaledSize(13, instance).dp.sp,
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
                            fontSize = StringUtils.scaledSize(13, instance).dp.sp,
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
                            fontSize = StringUtils.scaledSize(13, instance).dp.sp,
                            text = (if (Registry.getInstance(instance).language == "en") "Chance of Rain" else "降雨概率")
                                .plus(String.format(" %.0f", weatherInfo.chanceOfRain).plus("%"))
                        )
                    }
                }
            }
        }
        itemList.add(Section.DATE) {
            DateTimeElements(lunarDate, instance)
        }
        itemList.add {
            Spacer(modifier = Modifier.size(25.dp))
        }
        if (weatherWarnings.isEmpty() && weatherTips.isEmpty()) {
            itemList.add {
                LocalForecastButton(weatherInfo.weatherIcon, weatherInfo.nextWeatherIcon, weatherInfo.localForecastInfo, instance)
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        } else if (weatherInfo.specialTyphoonInfo?.hasAnyDisplay() == true) {
            itemList.add {
                StormLatestButton(instance, weatherInfo.specialTyphoonInfo!!)
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        }
        itemList.add(Section.WARNINGS) {
            Text(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 13F.sp,
                text = if (Registry.getInstance(instance).language == "en") "Weather Warnings" else "天氣警告"
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(4.dp))
        }
        if (weatherWarnings.isEmpty()) {
            itemList.add {
                Text(
                    modifier = Modifier.padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 10F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "There are currently no active warning signals." else "目前沒有任何天氣警告信號"
                )
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        } else {
            val list = weatherWarnings.toList()
            for (i in list.indices step 3) {
                itemList.add {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (u in i until (i + 3).coerceAtMost(list.size)) {
                            val (warning, details) = list[u]
                            Image(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(StringUtils.scaledSize(40, instance).dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (details == null) {
                                                instance.runOnUiThread {
                                                    Toast
                                                        .makeText(
                                                            instance,
                                                            if (Registry.getInstance(instance).language == "en") warning.nameEn else warning.nameZh,
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                }
                                            } else {
                                                val intent = Intent(
                                                    instance,
                                                    DisplayInfoTextActivity::class.java
                                                )
                                                intent.putExtra("imageDrawable", warning.iconId)
                                                intent.putExtra(
                                                    "imageWidth",
                                                    StringUtils.scaledSize(60, instance)
                                                )
                                                intent.putExtra(
                                                    "imageDescription",
                                                    if (Registry.getInstance(instance).language == "en") warning.nameEn else warning.nameZh
                                                )
                                                intent.putExtra("text", details)
                                                instance.startActivity(intent)
                                            }
                                        },
                                        onLongClick = {
                                            instance.runOnUiThread {
                                                Toast
                                                    .makeText(
                                                        instance,
                                                        if (Registry.getInstance(instance).language == "en") warning.nameEn else warning.nameZh,
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            }
                                        }
                                    ),
                                painter = painterResource(warning.iconId),
                                contentDescription = if (Registry.getInstance(instance).language == "en") warning.nameEn else warning.nameZh
                            )
                        }
                    }
                }
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        if (weatherInfo.heatStressAtWorkInfo != null) {
            val heatStressAtWorkInfo = weatherInfo.heatStressAtWorkInfo
            itemList.add(Section.HEAT_STRESS_AT_WORK_WARNING) {
                Text(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Heat Stress at Work Warning" else "工作暑熱警告"
                )
            }
            itemList.add {
                Spacer(modifier = Modifier.size(4.dp))
            }
            when (heatStressAtWorkInfo.action!!) {
                HeatStressAtWorkWarningAction.ISSUE -> {
                    val warningLevel = heatStressAtWorkInfo.warningsLevel!!
                    itemList.add {
                        Image(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(StringUtils.scaledSize(40, instance).dp)
                                .combinedClickable(
                                    onClick = {
                                        instance.runOnUiThread {
                                            Toast
                                                .makeText(
                                                    instance,
                                                    if (Registry.getInstance(instance).language == "en") warningLevel.nameEn else warningLevel.nameZh,
                                                    Toast.LENGTH_LONG
                                                )
                                                .show()
                                        }
                                    }
                                ),
                            painter = painterResource(warningLevel.iconId),
                            contentDescription = if (Registry.getInstance(instance).language == "en") warningLevel.nameEn else warningLevel.nameZh
                        )
                    }
                    itemList.add {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    itemList.add {
                        Text(
                            modifier = Modifier.padding(20.dp, 0.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = 15F.sp,
                            text = heatStressAtWorkInfo.description
                        )
                    }
                }
                HeatStressAtWorkWarningAction.CANCEL -> {
                    itemList.add {
                        Text(
                            modifier = Modifier.padding(20.dp, 0.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = 13F.sp,
                            text = heatStressAtWorkInfo.description
                        )
                    }
                }
            }
            itemList.add {
                Spacer(modifier = Modifier.size(4.dp))
            }
            val dateFormat = DateTimeFormatter.ofPattern(DateFormat.getDateFormat(instance).let { if (it is SimpleDateFormat) it.toPattern() else "d/M/yy" }
                .plus(" ").plus(DateFormat.getTimeFormat(instance).let { if (it is SimpleDateFormat) it.toPattern() else "HH:mm" }))
            itemList.add {
                Text(
                    modifier = Modifier.padding(5.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 10F.sp,
                    text = dateFormat.format(heatStressAtWorkInfo.issueTime)
                )
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        }
        itemList.add(Section.TIPS) {
            Text(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 13F.sp,
                text = if (Registry.getInstance(instance).language == "en") "Special Weather Tips" else "特別天氣提示"
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(4.dp))
        }
        if (weatherTips.isEmpty()) {
            itemList.add {
                Text(
                    modifier = Modifier.padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 10F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "There are currently no active special weather tips." else "目前沒有任何特別天氣提示"
                )
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        } else {
            for (tip in weatherTips) {
                itemList.add {
                    Text(
                        modifier = Modifier.padding(20.dp, 0.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 15F.sp,
                        text = tip.first
                    )
                }
                itemList.add {
                    Spacer(modifier = Modifier.size(4.dp))
                }
                val date = Date(tip.second)
                val lastUpdateText =
                    DateFormat.getDateFormat(instance).timeZone(Shared.HK_TIMEZONE)
                        .format(date)
                        .plus(" ")
                        .plus(
                            DateFormat.getTimeFormat(instance).timeZone(Shared.HK_TIMEZONE)
                                .format(date)
                        )
                itemList.add {
                    Text(
                        modifier = Modifier.padding(5.dp, 0.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 10F.sp,
                        text = lastUpdateText
                    )
                }
                itemList.add {
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        if (weatherWarnings.isNotEmpty() || weatherTips.isNotEmpty()) {
            itemList.add {
                LocalForecastButton(weatherInfo.weatherIcon, weatherInfo.nextWeatherIcon, weatherInfo.localForecastInfo, instance)
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        }
        var specialButton = false
        if (weatherWarnings.keys.any { it.category == WeatherWarningsCategory.WTCSGNL }) {
            itemList.add {
                StormTrackButton(instance, Color(0xFF460000))
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
            specialButton = true
        }
        if (weatherWarnings.keys.any { it.category == WeatherWarningsCategory.WRAIN || it == WeatherWarningsType.WFNTSA || it.isOnOrAboveTyphoonSignalEight }) {
            itemList.add {
                RadarButton(instance, Color(0xFF460000))
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
            itemList.add {
                RainfallMapButton(instance, Color(0xFF460000))
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
            specialButton = true
        }
        if (specialButton) {
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
        }
        itemList.add(Section.UVINDEX) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "UV Index" else "紫外線指數"
                )
            }
        }
        if (weatherInfo.uvIndex >= 0) {
            val uvIndexType = UVIndexType.getByValue(weatherInfo.uvIndex)
            itemList.add {
                Text(
                    textAlign = TextAlign.Center,
                    color = Color(uvIndexType.color),
                    fontSize = 25F.sp,
                    fontWeight = FontWeight.Bold,
                    text = String.format("%.1f", weatherInfo.uvIndex).plus(" ")
                        .plus(if (Registry.getInstance(instance).language == "en") uvIndexType.en else uvIndexType.zh)
                )
            }
        } else {
            itemList.add {
                Text(
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFF9625),
                    fontSize = 25F.sp,
                    fontWeight = FontWeight.Bold,
                    text = "-"
                )
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.HUMIDITY) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Relative Humidity" else "相對濕度"
                )
            }
        }
        itemList.add {
            Text(
                textAlign = TextAlign.Center,
                color = Color(0xFF3CB0FF),
                fontSize = 25F.sp,
                fontWeight = FontWeight.Bold,
                text = String.format("%.0f", weatherInfo.currentHumidity).plus("%")
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.WIND) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Wind" else "風向風速"
                )
            }
        }
        val windText = if (weatherInfo.windSpeed < 0F) {
            "-"
        } else {
            weatherInfo.windDirection.plus(" ")
                .plus(String.format("%.0f", weatherInfo.windSpeed)).plus(if (Registry.getInstance(instance).language == "en") " km/h" else "公里/小時")
        }
        itemList.add(Section.WIND) {
            Box (
                modifier = Modifier.fillMaxWidth(0.9F),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFFFFF),
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = 25F.sp
                    ),
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    text = windText
                )
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.WIND) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Gust" else "陣風"
                )
            }
        }
        val gustText = if (weatherInfo.windSpeed < 0F) {
            "-"
        } else {
            String.format("%.0f", weatherInfo.gust).plus(if (Registry.getInstance(instance).language == "en") " km/h" else "公里/小時")
        }
        itemList.add(Section.WIND) {
            Box (
                modifier = Modifier.fillMaxWidth(0.9F),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFFFFF),
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = 25F.sp
                    ),
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    text = gustText
                )
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.SUN) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Sunrise" else "日出"
                )
            }
        }
        itemList.add(Section.SUN) {
            Text(
                textAlign = TextAlign.Center,
                color = Color(0xFFFFC32B),
                fontSize = 25F.sp,
                fontWeight = FontWeight.Bold,
                text = weatherInfo.sunriseTime.format(timeFormat)
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.SUN) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Sunset" else "日落"
                )
            }
        }
        itemList.add(Section.SUN) {
            Text(
                textAlign = TextAlign.Center,
                color = Color(0xFFFF802B),
                fontSize = 25F.sp,
                fontWeight = FontWeight.Bold,
                text = weatherInfo.sunsetTime.format(timeFormat)
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add(Section.MOON) {
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Moon Phase" else "月相"
                )
            }
        }
        itemList.add(Section.MOON) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .padding(8.sp.dp)
                    .size(25.sp.dp),
                loading = {
                    CircularProgressIndicator(
                        color = Color.LightGray,
                        strokeWidth = 3.dp,
                        trackColor = Color.DarkGray,
                        strokeCap = StrokeCap.Round,
                    )
                },
                model = ImageRequest.Builder(instance).size(30, 30).data(moonPhaseUrl).build(),
                contentDescription = if (Registry.getInstance(instance).language == "en") "Moon Phase" else "月相"
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Moonrise" else "月出"
                )
            }
            Text(
                textAlign = TextAlign.Center,
                color = Color(0xFFDFDFDF),
                fontSize = 25F.sp,
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
                    fontSize = 13F.sp,
                    text = if (Registry.getInstance(instance).language == "en") "Moonset" else "月落"
                )
            }
            Text(
                textAlign = TextAlign.Center,
                color = Color(0xFFBEBEBE),
                fontSize = 25F.sp,
                fontWeight = FontWeight.Bold,
                text = if (weatherInfo.moonsetTime == null) "-" else weatherInfo.moonsetTime.format(timeFormat)
            )
        }
        if (weatherInfo.moonriseTime == null || weatherInfo.moonsetTime == null || weatherInfo.moonriseTime.isBefore(weatherInfo.moonsetTime)) {
            itemList.add(Section.MOON) {
                moonrise()
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
            itemList.add(Section.MOON) {
                moonset()
            }
        } else {
            itemList.add(Section.MOON) {
                moonset()
            }
            itemList.add {
                Spacer(modifier = Modifier.size(10.dp))
            }
            itemList.add(Section.MOON) {
                moonrise()
            }
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add {
            Spacer(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF333333))
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add {
            RadarButton(instance)
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add {
            RainfallMapButton(instance)
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.add {
            StormTrackButton(instance)
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add {
            Spacer(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF333333))
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.addAll(Section.HOURLY,
            generateHourlyItems(weatherInfo, timeFormat, instance)
        )
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add {
            Spacer(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF333333))
            )
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add(Section.FORECAST) {
            ForecastGeneralButton(weatherInfo.forecastGeneralSituation, instance)
        }
        itemList.add {
            Spacer(modifier = Modifier.size(10.dp))
        }
        itemList.addAll(Section.FORECAST,
            generateForecastItems(weatherInfo, instance)
        )
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
        itemList.add {
            Spacer(modifier = Modifier
                .padding(20.dp, 0.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF333333)))
        }
        itemList.add {
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
    return itemList
}

@OptIn(ExperimentalFoundationApi::class)
fun generateHourlyItems(weatherInfo: CurrentWeatherInfo, timeFormat: DateTimeFormatter, instance: TitleActivity): List<@Composable () -> Unit> {
    val itemList: MutableList<@Composable () -> Unit> = ArrayList()
    var count = 0
    val nowHour = LocalDateTime.now(Shared.HK_TIMEZONE.toZoneId())
    for (hourInfo in weatherInfo.hourlyWeatherInfo) {
        if (hourInfo.time.isBefore(nowHour)) {
            continue
        }
        if (++count > 12) {
            break
        }
        itemList.add {
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            val weatherIcon = hourInfo.weatherIcon
                            val weatherDescription =
                                if (Registry.getInstance(instance).language == "en") weatherIcon.descriptionEn else weatherIcon.descriptionZh
                            val intent = Intent(instance, DisplayInfoTextActivity::class.java)
                            intent.putExtra("imageDrawable", weatherIcon.iconId)
                            intent.putExtra("imageWidth", StringUtils.scaledSize(60, instance))
                            intent.putExtra("imageDescription", weatherDescription)
                            intent.putExtra("text", hourInfo.toDisplayText(instance))
                            instance.startActivity(intent)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val small = ScreenSizeUtils.getScreenWidth(instance) < 450
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 14F.sp.clamp(max = 14.dp),
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
                            fontSize = (size - 1F).sp.clamp(max = (size - 1).dp),
                            text = String.format("%.0f", hourInfo.humidity).plus("%").plus(if (small) " " else "  ")
                        )
                    }
                    Image(
                        modifier = Modifier
                            .padding(1.dp, 1.dp)
                            .size(20.dp)
                            .combinedClickable(
                                onClick = {
                                    instance.runOnUiThread {
                                        Toast
                                            .makeText(
                                                instance,
                                                if (Registry.getInstance(instance).language == "en") hourInfo.weatherIcon.descriptionEn else hourInfo.weatherIcon.descriptionZh,
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    }
                                }
                            ),
                        painter = painterResource(hourInfo.weatherIcon.iconId),
                        contentDescription = if (Registry.getInstance(instance).language == "en") hourInfo.weatherIcon.descriptionEn else hourInfo.weatherIcon.descriptionZh
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 14F.sp.clamp(max = 14.dp),
                        fontWeight = FontWeight.Bold,
                        text = String.format("%.1f", hourInfo.temperature).plus("°")
                    )
                }
            }
        }
    }
    return itemList
}

@OptIn(ExperimentalFoundationApi::class)
fun generateForecastItems(weatherInfo: CurrentWeatherInfo, instance: TitleActivity): List<@Composable () -> Unit> {
    val itemList: MutableList<@Composable () -> Unit> = ArrayList()
    for (dayInfo in weatherInfo.forecastInfo) {
        itemList.add {
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            val weatherIcon = dayInfo.weatherIcon
                            val weatherDescription =
                                if (Registry.getInstance(instance).language == "en") weatherIcon.descriptionEn else weatherIcon.descriptionZh
                            val intent = Intent(instance, DisplayInfoTextActivity::class.java)
                            intent.putExtra("imageDrawable", weatherIcon.iconId)
                            intent.putExtra("imageWidth", StringUtils.scaledSize(60, instance))
                            intent.putExtra("imageDescription", weatherDescription)
                            intent.putExtra("text", dayInfo.toDisplayText(instance))
                            instance.startActivity(intent)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.primary,
                        fontSize = 14F.sp.clamp(max = 14.dp),
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
                        fontSize = 9F.sp.clamp(max = 9.dp),
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
                        fontSize = 9F.sp.clamp(max = 9.dp),
                        text = if (dayInfo.chanceOfRain >= 0) String.format(
                            "%.0f",
                            dayInfo.chanceOfRain
                        ).plus("%") else "??%"
                    )
                    Image(
                        modifier = Modifier
                            .padding(1.dp, 1.dp)
                            .size(20.dp)
                            .combinedClickable(
                                onClick = {
                                    instance.runOnUiThread {
                                        Toast
                                            .makeText(
                                                instance,
                                                if (Registry.getInstance(instance).language == "en") dayInfo.weatherIcon.descriptionEn else dayInfo.weatherIcon.descriptionZh,
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    }
                                }
                            ),
                        painter = painterResource(dayInfo.weatherIcon.iconId),
                        contentDescription = if (Registry.getInstance(instance).language == "en") dayInfo.weatherIcon.descriptionEn else dayInfo.weatherIcon.descriptionZh
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 14F.sp.clamp(max = 14.dp),
                        fontWeight = FontWeight.Bold,
                        text = String.format("%.0f", dayInfo.lowestTemperature).plus("-")
                            .plus(String.format("%.0f", dayInfo.highestTemperature))
                            .plus("°")
                    )
                }
            }
        }
    }
    return itemList
}

@Composable
fun LocalForecastButton(weatherIcon: WeatherStatusIcon, nextWeatherIcon: WeatherStatusIcon?, localForecastInfo: LocalForecastInfo, instance: TitleActivity, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            var weatherDescription = if (Registry.getInstance(instance).language == "en") weatherIcon.descriptionEn else weatherIcon.descriptionZh
            if (nextWeatherIcon != null) {
                weatherDescription = weatherDescription.plus(if (Registry.getInstance(instance).language == "en") " to ".plus(nextWeatherIcon.descriptionEn) else " 至 ".plus(nextWeatherIcon.descriptionZh))
            }
            val text = (if (Registry.getInstance(instance).language == "en") "Now: " else "現時 ").plus(weatherDescription).plus("\n").plus(localForecastInfo.toDisplayText(instance))
            val intent = Intent(instance, DisplayInfoTextActivity::class.java)
            if (nextWeatherIcon == null) {
                intent.putExtra("imageDrawable", weatherIcon.iconId)
            } else {
                intent.putExtra("imageDrawables", intArrayOf(weatherIcon.iconId, R.mipmap.towards, nextWeatherIcon.iconId))
            }
            intent.putExtra("imageHeight", StringUtils.scaledSize(60, instance))
            intent.putExtra("imageDescription", weatherDescription)
            intent.putExtra("text", text)
            intent.putExtra("footer", localForecastInfo.toDisplayFooter(instance))
            instance.startActivity(intent)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.weather),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Local Forecast" else "本港預報"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "Local Forecast" else "本港預報"
                )
            }
        }
    )
}

@Composable
fun StormLatestButton(instance: TitleActivity, specialTyphoonInfo: SpecialTyphoonInfo, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            val intent = Intent(instance, DisplayInfoTextActivity::class.java)
            specialTyphoonInfo.signalType?.let {
                intent.putExtra("imageDrawable", it.iconId)
                intent.putExtra("imageWidth", StringUtils.scaledSize(60, instance))
                intent.putExtra("imageDescription", if (Registry.getInstance(instance).language == "en") it.nameEn else it.nameZh)
            }
            intent.putExtra("text", (if (Registry.getInstance(instance).language == "en") "Latest TC News" else "最新風暴消息").plus("\n").plus(specialTyphoonInfo.toDisplayText()))
            instance.startActivity(intent)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.cyclone_latest),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Latest TC News" else "最新風暴消息"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "Latest TC News" else "最新風暴消息"
                )
            }
        }
    )
}

@Composable
fun ForecastGeneralButton(forecastGeneralSituation: String, instance: TitleActivity, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            val intent = Intent(instance, DisplayInfoTextActivity::class.java)
            intent.putExtra("text", (if (Registry.getInstance(instance).language == "en") "9-Day General Forecast" else "展望未來九天天氣概況").plus("\n").plus(forecastGeneralSituation))
            instance.startActivity(intent)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.forecast),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "General Forecast" else "展望天氣概況"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "General Forecast" else "展望天氣概況"
                )
            }
        }
    )
}

@Composable
fun RadarButton(instance: TitleActivity, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, RadarActivity::class.java))
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.radar),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Radar Image (64 km)" else "雷達圖像 (64公里)"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "Radar Image (64 km)" else "雷達圖像 (64公里)"
                )
            }
        }
    )
}

@Composable
fun RainfallMapButton(instance: TitleActivity, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, RainfallMapActivity::class.java))
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.rainfall),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Rainfall Dist. Maps" else "雨量分佈圖"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "Rainfall Dist. Maps" else "雨量分佈圖"
                )
            }
        }
    )
}

@Composable
fun StormTrackButton(instance: TitleActivity, backgroundColor: Color = MaterialTheme.colors.secondary) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, TCTrackActivity::class.java))
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(180, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                        .size(15.sp.clamp(max = 15.dp).dp),
                    painter = painterResource(R.mipmap.cyclone),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Storm Track" else "風暴路徑"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "Storm Track" else "風暴路徑"
                )
            }
        }
    )
}

fun openHKOApp(instance: TitleActivity) {
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
}

@Composable
fun OpenHKOAppButton(instance: TitleActivity) {
    Button(
        onClick = {
            openHKOApp(instance)
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            AutoResizeText(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSizeRange = FontSizeRange(
                    min = 1F.sp,
                    max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                ),
                maxLines = 2,
                text = if (Registry.getInstance(instance).language == "en") "Open MyObservatory" else "開啟我的天文台"
            )
        }
    )
}

@Composable
fun ChangeLocationButton(instance: TitleActivity, enabled: Boolean) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, ChangeLocationActivity::class.java))
        },
        enabled = enabled,
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            AutoResizeText(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSizeRange = FontSizeRange(
                    min = 1F.sp,
                    max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                ),
                maxLines = 2,
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
            .padding(20.dp, 0.dp)
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
            AutoResizeText(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSizeRange = FontSizeRange(
                    min = 1F.sp,
                    max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                ),
                maxLines = 2,
                text = (if (Registry.getInstance(instance).language == "en") "Set Refresh Rate: " else "更新頻率: ").plus(timeText)
            )
        }
    )
}

@Composable
fun LanguageButton(instance: TitleActivity, enabled: Boolean) {
    Button(
        onClick = {
            Registry.getInstance(instance).setLanguage(if (Registry.getInstance(instance).language == "en") "zh" else "en", instance)
            Shared.currentWeatherInfo.reset(instance)
            Shared.currentWarnings.reset(instance)
            Shared.currentTips.reset(instance)
            instance.startActivity(Intent(instance, TitleActivity::class.java))
            instance.finishAffinity()
        },
        enabled = enabled,
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
                fontSize = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp),
                text = if (Registry.getInstance(instance).language == "en") "中文" else "English"
            )
        }
    )
}

@Composable
fun UpdateTilesButton(updating: Boolean, instance: TitleActivity) {
    Button(
        onClick = {
            Shared.currentWeatherInfo.reset(instance)
            Shared.currentWarnings.reset(instance)
            Shared.currentTips.reset(instance)
            Shared.currentWeatherInfo.getLatestValue(instance, ForkJoinPool.commonPool(), true)
            Shared.currentWarnings.getLatestValue(instance, ForkJoinPool.commonPool(), true)
            Shared.currentTips.getLatestValue(instance, ForkJoinPool.commonPool(), true)
            instance.runOnUiThread {
                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Refreshing..." else "正在更新...", Toast.LENGTH_SHORT).show()
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
                painter = painterResource(if (updating) R.mipmap.reloading else R.mipmap.reload),
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
            min = 1F.sp,
            max = 15F.sp
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