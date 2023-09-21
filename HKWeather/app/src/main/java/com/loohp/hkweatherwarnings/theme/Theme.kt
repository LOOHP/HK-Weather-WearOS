package com.loohp.hkweatherwarnings.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import com.loohp.hkweatherwarnings.theme.Typography
import com.loohp.hkweatherwarnings.theme.wearColorPalette

@Composable
fun HKWeatherWarningsTheme(
        content: @Composable () -> Unit
) {
    MaterialTheme(
            colors = wearColorPalette,
            typography = Typography,
            // For shapes, we generally recommend using the default Material Wear shapes which are
            // optimized for round and non-round devices.
            content = content
    )
}