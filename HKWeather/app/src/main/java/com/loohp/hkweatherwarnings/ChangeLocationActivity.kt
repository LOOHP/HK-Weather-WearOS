package com.loohp.hkweatherwarnings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.compose.verticalScrollWithScrollbar
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.shared.Shared
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.LocationUtils
import com.loohp.hkweatherwarnings.utils.LocationUtils.LocationResult
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.clamp
import com.loohp.hkweatherwarnings.utils.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ChangeLocationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainElements(this)
        }
    }

}

@Composable
fun MainElements(instance: ChangeLocationActivity) {
    var requestingLocation by remember { mutableStateOf(false) }

    HKWeatherTheme {
        if (requestingLocation) {
            Box (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp, 30.dp, 30.dp, 20.dp)
            ) {
                AutoResizeText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8F)
                        .align(Alignment.TopCenter),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = TextUnit(1F, TextUnitType.Sp),
                        max = StringUtils.scaledSize(17F, instance).sp.clamp(max = 18.dp)
                    ),
                    text = if (Registry.getInstance(instance).language == "en") {
                                "Using GPS Location requires background location permission to locate your current district, no location data are stored or sent.\n\n" +
                                "If prompted, please choose \"While using this app\" and then \"All the time\"."
                    } else {
                                "使用你的位置功能需要在背景存取定位位置的權限搜尋你目前所在的地區 程式不會儲存或發送位置數據\n\n" +
                                "如出現權限請求 請分別選擇「僅限使用應用程式時」和「一律允許」"
                    }
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Button(
                    onClick = {
                        if (LocationUtils.checkLocationPermission(instance) {
                                if (it) {
                                    Registry.getInstance(instance).setLocationGPS(instance)
                                    Shared.currentWeatherInfo.reset()
                                    Shared.currentWarnings.reset()
                                    Shared.currentTips.reset()
                                    instance.startActivity(Intent(instance, TitleActivity::class.java))
                                    instance.finishAffinity()
                                } else {
                                    instance.runOnUiThread {
                                        Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                            Registry.getInstance(instance).setLocationGPS(instance)
                            Shared.currentWeatherInfo.reset()
                            Shared.currentWarnings.reset()
                            Shared.currentTips.reset()
                            instance.startActivity(Intent(instance, TitleActivity::class.java))
                            instance.finishAffinity()
                        }
                        requestingLocation = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7F)
                        .fillMaxHeight(0.2F)
                        .align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(16F, TextUnitType.Sp),
                            text = if (Registry.getInstance(instance).language == "en") "Continue" else "繼續"
                        )
                    }
                )
            }
        } else {
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
                    .focusable(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
                AutoResizeText(
                    modifier = Modifier
                        .width(StringUtils.scaledSize(180, instance).dp)
                        .padding(5.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    maxLines = 1,
                    fontSizeRange = FontSizeRange(
                        min = TextUnit(1F, TextUnitType.Sp),
                        max = TextUnit(15F, TextUnitType.Sp)
                    ),
                    text = if (Registry.getInstance(instance).language == "en") "Choose Weather Location" else "選擇天氣資訊位置"
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(17, instance).dp))
                Button(
                    onClick = {
                        Registry.getInstance(instance).clearLocation(instance)
                        Shared.currentWeatherInfo.reset()
                        Shared.currentWarnings.reset()
                        Shared.currentTips.reset()
                        instance.startActivity(Intent(instance, TitleActivity::class.java))
                        instance.finishAffinity()
                    },
                    modifier = Modifier
                        .width(StringUtils.scaledSize(180, instance).dp)
                        .height(StringUtils.scaledSize(55, instance).dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(0.9F)
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(16F, TextUnitType.Sp).clamp(max = 16.dp),
                            text = if (Registry.getInstance(instance).language == "en") "Hong Kong" else "本港"
                        )
                    }
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Button(
                    onClick = {
                        requestingLocation = true
                    },
                    modifier = Modifier
                        .width(StringUtils.scaledSize(180, instance).dp)
                        .height(StringUtils.scaledSize(55, instance).dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth(0.9F)
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Image(
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 2.dp, 0.dp)
                                    .size(16.sp.clamp(max = 16.dp).dp),
                                painter = painterResource(R.mipmap.gps),
                                contentDescription = if (Registry.getInstance(instance).language == "en") "GPS Location" else "你的位置"
                            )
                            Text(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontSize = TextUnit(16F, TextUnitType.Sp).clamp(max = 16.dp),
                                text = if (Registry.getInstance(instance).language == "en") "GPS Location" else "你的位置"
                            )
                        }
                    }
                )
                val stations = Registry.getInstance(instance).weatherStations.sortedBy { -it.optJSONObject("geometry")!!.optJSONArray("coordinates")!!.optDouble(1) }
                for (station in stations) {
                    val properties = station.optJSONObject("properties")!!
                    val name = properties.optString("AutomaticWeatherStation_".plus(if (Registry.getInstance(instance).language == "en") "en" else "uc"))
                    val coordinates = station.optJSONObject("geometry")!!.optJSONArray("coordinates")!!
                    val location = LocationResult.fromLatLng(coordinates.optDouble(1), coordinates.optDouble(0)).location
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                    Button(
                        onClick = {
                            Registry.getInstance(instance).setLocation(location, instance)
                            Shared.currentWeatherInfo.reset()
                            Shared.currentWarnings.reset()
                            Shared.currentTips.reset()
                            instance.startActivity(Intent(instance, TitleActivity::class.java))
                            instance.finishAffinity()
                        },
                        modifier = Modifier
                            .width(StringUtils.scaledSize(180, instance).dp)
                            .height(StringUtils.scaledSize(55, instance).dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.primary
                        ),
                        content = {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(0.9F)
                                    .align(Alignment.Center),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontSize = TextUnit(16F, TextUnitType.Sp).clamp(max = 16.dp),
                                text = name
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(25, instance).dp))
            }
        }
    }
}