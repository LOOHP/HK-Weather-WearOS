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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.loohp.hkweatherwarnings.shared.Shared


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Shared.startBackgroundService(this)
        setContent {
            LaunchedEffect (Unit) {
                val launchIntent = Intent(this@MainActivity, TitleActivity::class.java)
                if (intent.extras != null && intent.extras!!.containsKey("launchSection")) {
                    launchIntent.putExtra("launchSection", intent.extras!!.getString("launchSection"))
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(launchIntent)
                if (intent.extras != null && intent.extras!!.getInt("warningInfo", 0) > 0) {
                    val infoIndex = Intent(this@MainActivity, DisplayInfoTextActivity::class.java)
                    infoIndex.putExtras(intent.extras!!)
                    this@MainActivity.startActivity(infoIndex)
                }
                finishAffinity()
            }
        }
    }

}