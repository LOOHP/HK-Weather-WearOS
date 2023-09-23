package com.loohp.hkweatherwarnings.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.ForkJoinPool

class PeriodicUpdateWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            Shared.currentWeatherInfo.getLatestValue(context, ForkJoinPool.commonPool(), true)
            Shared.currentWarnings.getLatestValue(context, ForkJoinPool.commonPool(), true)
            Shared.currentTips.getLatestValue(context, ForkJoinPool.commonPool(), true)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

}