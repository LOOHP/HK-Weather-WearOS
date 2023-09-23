package com.loohp.hkweatherwarnings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_FLEX_MILLIS
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkweatherwarnings.background.PeriodicUpdateWorker
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val updateRequest = PeriodicWorkRequestBuilder<PeriodicUpdateWorker>(Shared.REFRESH_INTERVAL, TimeUnit.MILLISECONDS, MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(Shared.BACKGROUND_SERVICE_REQUEST_TAG, ExistingPeriodicWorkPolicy.UPDATE, updateRequest)

        setContent {
            LaunchedEffect (Unit) {
                startActivity(Intent(this@MainActivity, TitleActivity::class.java))
                finishAffinity()
            }
        }
    }

}