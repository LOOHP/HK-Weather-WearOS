package com.loohp.hkweatherwarnings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.loohp.hkweatherwarnings.compose.verticalScrollWithScrollbar
import com.loohp.hkweatherwarnings.theme.HKWeatherTheme
import com.loohp.hkweatherwarnings.utils.StringUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val emptyIntArray = IntArray(0)

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
            DisplayInfo(imageDrawables, imageUrl, imageWidth, imageHeight, imageDescription, text, footer, this)
        }
    }

}

@Composable
fun DisplayInfo(imageDrawables: IntArray, imageUrl: String?, imageWidth: Int, imageHeight: Int, imageDescription: String, text: String, footer: String, instance: DisplayInfoTextActivity) {
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
                .verticalScrollWithScrollbar(
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
                if (imageDrawables.isNotEmpty()) {
                    Row (
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (imageDrawable in imageDrawables) {
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
                    fontSize = TextUnit(StringUtils.scaledSize(15F, instance), TextUnitType.Sp),
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
                    fontSize = TextUnit(StringUtils.scaledSize(10F, instance), TextUnitType.Sp),
                    text = footer
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
        }
    }
}