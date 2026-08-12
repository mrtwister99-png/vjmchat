package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notification.SystemNotificationHelper
import com.example.worker.VJMNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun schedulePeriodicWork(context: android.content.Context) {
        SystemNotificationHelper.createChannels(context)

        try { WorkManager.getInstance(context).cancelUniqueWork("vjm_notifications_test") } catch (_: Exception) {}

        val request = PeriodicWorkRequestBuilder<VJMNotificationWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "vjm_notifications",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun hasNotificationPermission(context: android.content.Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}