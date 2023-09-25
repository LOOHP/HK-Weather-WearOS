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
                startActivity(Intent(this@MainActivity, TitleActivity::class.java))
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