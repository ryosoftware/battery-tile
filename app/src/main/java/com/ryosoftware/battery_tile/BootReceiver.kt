package com.ryosoftware.battery_tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val logger = Main.from(context).logger

        logger.log("Received a ${action?.substringAfterLast(".")} event")

        if (action != Intent.ACTION_BOOT_COMPLETED) return

        NotificationService.resetStats(context, LastStatsResetReason.DEVICE_REBOOT)

        NotificationService.runOrStop(context)
    }
}
