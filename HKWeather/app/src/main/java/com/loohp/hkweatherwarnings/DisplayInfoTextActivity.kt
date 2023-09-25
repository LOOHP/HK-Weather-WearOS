package com.loohp.hkweatherwarnings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
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


class DisplayInfoTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageDrawable = intent.extras!!.getInt("imageDrawable", -1)
        val imageUrl = intent.extras!!.getString("imageUrl")
        val imageWidth = intent.extras!!.getInt("imageWidth", -1)
        val imageHeight = intent.extras!!.getInt("imageHeight", -1)
        val imageDescription = intent.extras!!.getString("imageDescription", "")
        val text = intent.extras!!.getString("text", "")

        setContent {
            DisplayInfo(imageDrawable, imageUrl, imageWidth, imageHeight, imageDescription, text, this)
        }
    }

}

@Composable
fun DisplayInfo(imageDrawable: Int, imageUrl: String?, imageWidth: Int, imageHeight: Int, imageDescription: String, text: String, instance: DisplayInfoTextActivity) {
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
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
            if (imageWidth >= 0) {
                var modifier = Modifier
                    .width(imageWidth.dp)
                if (imageHeight >= 0) {
                    modifier = modifier
                        .height(imageHeight.dp)
                }
                if (imageDrawable >= 0) {
                    Image(
                        modifier = modifier,
                        painter = painterResource(imageDrawable),
                        contentDescription = imageDescription
                    )
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                } else if (imageUrl != null) {
                    AsyncImage(
                        modifier = modifier,
                        model = imageUrl,
                        contentDescription = imageDescription
                    )
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                }
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
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            }
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
        }
    }
}