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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.loohp.hkweatherwarnings.compose.AutoResizeText
import com.loohp.hkweatherwarnings.compose.FontSizeRange
import com.loohp.hkweatherwarnings.compose.fullPageVerticalScrollWithScrollbar
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.ImmutableState
import com.loohp.hkweatherwarnings.utils.RemoteActivityUtils
import com.loohp.hkweatherwarnings.utils.StringUtils
import com.loohp.hkweatherwarnings.utils.asImmutableState
import com.loohp.hkweatherwarnings.utils.clamp
import com.loohp.hkweatherwarnings.utils.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val emptyIntArray = IntArray(0)

@Stable
class DisplayInfoTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var imageDrawables = intent.extras!!.getIntArray("imageDrawables")?: emptyIntArray
        if (imageDrawables.isEmpty()) {
            val imageDrawable = intent.extras!!.getInt("imageDrawable", -1)
            if (imageDrawable >= 0) {
                imageDrawables = intArrayOf(imageDrawable)
            }
        }
        val imageUrl = intent.extras!!.getString("imageUrl")
        val imageWidth = intent.extras!!.getInt("imageWidth", -1)
        val imageHeight = intent.extras!!.getInt("imageHeight", -1)
        val imageDescription = intent.extras!!.getString("imageDescription", "")
        val text = intent.extras!!.getString("text", "")
        val footer = intent.extras!!.getString("footer", "")

        setContent {
            DisplayInfo(imageDrawables.asImmutableState(), imageUrl, imageWidth, imageHeight, imageDescription, text, footer, this)
        }
    }

}

@Composable
fun DisplayInfo(imageDrawables: ImmutableState<IntArray>, imageUrl: String?, imageWidth: Int, imageHeight: Int, imageDescription: String, text: String, footer: String, instance: DisplayInfoTextActivity) {
    HKWeatherTheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(false) }

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
            if (scrollReachedEnd && scrollMoved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (!scrollMoved) {
                scrollMoved = true
            }
        }
        LaunchedEffect (Unit) {
            focusRequester.requestFocus()
        }

        Column (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalScrollWithScrollbar(
                    state = scroll,
                    flingBehavior = ScrollableDefaults.flingBehavior()
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
            if (imageWidth >= 0 || imageHeight >= 0) {
                var modifier: Modifier? = null
                if (imageWidth >= 0) {
                    modifier = Modifier.width(imageWidth.dp)
                }
                if (imageHeight >= 0) {
                    modifier = (modifier?: Modifier).height(imageHeight.dp)
                }
                if (imageDrawables.value.isNotEmpty()) {
                    Row (
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (imageDrawable in imageDrawables.value) {
                            Image(
                                modifier = modifier!!,
                                painter = painterResource(imageDrawable),
                                contentDescription = imageDescription
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                } else if (imageUrl != null) {
                    AsyncImage(
                        modifier = modifier!!,
                        model = imageUrl,
                        contentDescription = imageDescription
                    )
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                }
            } else {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(30, instance).dp))
            }
            for ((i, line) in text.split(Regex("\\R")).withIndex()) {
                Text(
                    modifier = Modifier.fillMaxWidth(0.8F),
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.primary,
                    fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = StringUtils.scaledSize(15F, instance).sp,
                    text = line
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            if (footer.isNotEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth(0.8F),
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize = StringUtils.scaledSize(10F, instance).sp,
                    text = footer
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            Spacer(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF333333))
            )
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            OpenHKOAppButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(50, instance).dp))
        }
    }
}

fun openHKOApp(instance: DisplayInfoTextActivity) {
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
fun OpenHKOAppButton(instance: DisplayInfoTextActivity) {
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
                    painter = painterResource(R.mipmap.hko_icon),
                    contentDescription = if (Registry.getInstance(instance).language == "en") "Open MyObservatory" else "開啟我的天文台"
                )
                AutoResizeText(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(
                        min = 1F.sp,
                        max = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp)
                    ),
                    maxLines = 1,
                    text = if (Registry.getInstance(instance).language == "en") "See Details" else "參閱詳情"
                )
            }
        }
    )
}