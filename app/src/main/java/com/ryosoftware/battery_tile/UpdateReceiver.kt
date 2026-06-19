package com.ryosoftware.battery_tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val logger = Main.from(context).logger

        logger.log("Received a ${action?.substringAfterLast(".")} event")

        if (action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        NotificationService.runOrStop(context)
    }
}
