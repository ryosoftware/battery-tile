package com.ryosoftware.battery_tile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.edit

class Main : Application() {
    lateinit var logger: Logger
        private set

    lateinit var batteryIntentProvider: BatteryIntentProvider
        private set

    companion object {
        private const val FILENAME = "main_prefs"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification-permission-requested"

        fun from(context: Context): Main = context.applicationContext as Main

        fun Context.hasPostNotificationsPermission() : Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun Activity.showRequestPostNotificationsPermissionReason() {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notifications_dialog_title)
                .setMessage(R.string.notifications_dialog_body)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                    startActivity(intent)
                }
                .show()
        }

        @SuppressLint("InlinedApi")
        fun Activity.requestPostNotificationsPermission(requestCode: Int) {
            val prefs = getSharedPreferences(FILENAME, Context.MODE_PRIVATE)
            val wasRequested = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)

            if (wasRequested && (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
                showRequestPostNotificationsPermissionReason()
            } else {
                prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true) }
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
            }
        }

        fun Activity.requestPostNotificationsPermission() = requestPostNotificationsPermission(0)

        fun Context.hasBatteryOptimizationBypassPermission(): Boolean =
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

        @SuppressLint("InlinedApi", "BatteryLife")
        fun Context.requestBypassBatteryOptimizationPermission() {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            }

            startActivity(intent)
        }

        fun Context.hasExactAlarmPermission(): Boolean =
           Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
           getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

        @SuppressLint("InlinedApi")
        fun Context.requestPostExactAlarmPermission() {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:$packageName".toUri()
            }

            startActivity(intent)
        }

        fun Context.isScreenOn(): Boolean = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive

        fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

        fun Context.logAppVersion() {
            val logger = Main.from(this).logger

            logger.log("App version is ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) and OS version is ${Build.VERSION.SDK_INT}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger = FileLogger(this)
        batteryIntentProvider = CachedBatteryIntentProvider(this)

        logAppVersion()

        createNotificationChannels()
    }

    private fun createNotificationChannel(channelId: String, @StringRes nameRes: Int, importance: Int) {
        val manager = getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                getString(nameRes),
                importance
            )
            manager.createNotificationChannel(channel)
        }
    }
    private fun createNotificationChannels() {
        createNotificationChannel(NotificationService.CHANNEL_ID, R.string.background_service_notification, NotificationManager.IMPORTANCE_LOW)
        createNotificationChannel(NotificationService.POWER_CONNECTED_CHANNEL_ID, R.string.power_connected_notification, NotificationManager.IMPORTANCE_HIGH)
        createNotificationChannel(NotificationService.POWER_DISCONNECTED_CHANNEL_ID, R.string.power_disconnected_notification, NotificationManager.IMPORTANCE_HIGH)
        createNotificationChannel(NotificationService.BATTERY_CHARGED_CHANNEL_ID, R.string.battery_charged_notification, NotificationManager.IMPORTANCE_HIGH)
        createNotificationChannel(NotificationService.BATTERY_LOW_CHANNEL_ID, R.string.battery_low_notification, NotificationManager.IMPORTANCE_HIGH)
        createNotificationChannel(NotificationService.BATTERY_TEMPERATURE_WARNING_CHANNEL_ID, R.string.battery_temperature_notification, NotificationManager.IMPORTANCE_HIGH)
        createNotificationChannel(NotificationService.BATTERY_HEALTH_WARNING_CHANNEL_ID, R.string.battery_health_notification, NotificationManager.IMPORTANCE_HIGH)
    }
}
