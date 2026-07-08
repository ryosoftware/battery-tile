package com.ryosoftware.battery_tile

import android.content.Context

class PreferencesModule(context: Context) {
    val prefs by lazy {
        listOf(
            AppPreferences(context),
            BatteryTilePreferences(context),
            NotificationPreferences(context),
            NotificationServicePreferences(context)
        )
    }
}