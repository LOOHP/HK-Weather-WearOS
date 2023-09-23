package com.loohp.hkweatherwarnings.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.loohp.hkweatherwarnings.shared.Shared


class WorkManagerStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> Shared.startBackgroundService(context)
            "android.intent.action.QUICKBOOT_POWERON" -> Shared.startBackgroundService(context)
        }
    }

}