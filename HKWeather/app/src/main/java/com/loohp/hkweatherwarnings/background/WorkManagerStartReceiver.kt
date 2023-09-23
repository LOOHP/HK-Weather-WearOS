package com.loohp.hkweatherwarnings.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_FLEX_MILLIS
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.TimeUnit


class WorkManagerStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> startBackgroundService(context)
            "android.intent.action.QUICKBOOT_POWERON" -> startBackgroundService(context)
        }
    }

    private fun startBackgroundService(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<PeriodicUpdateWorker>(Shared.REFRESH_INTERVAL, TimeUnit.MILLISECONDS, MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(Shared.BACKGROUND_SERVICE_REQUEST_TAG, ExistingPeriodicWorkPolicy.UPDATE, updateRequest)
    }

}