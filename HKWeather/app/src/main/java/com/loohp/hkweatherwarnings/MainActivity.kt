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