package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.LocaleList
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.ryosoftware.battery_tile.Main.Companion.hasPostNotificationsPermission
import com.ryosoftware.battery_tile.Main.Companion.isScreenOn
import com.ryosoftware.battery_tile.NotificationBatteryIntentHelper.NotificationField.Companion.getLabel
import com.ryosoftware.battery_tile.WhatAppOpens.Companion.getIntent
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.BatteryRepository
import com.ryosoftware.battery_tile.data.ChargingSession
import com.ryosoftware.battery_tile.data.ScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LastStatsResetReason(val key: String) {
    POWER_DISCONNECTED(key = "POWER-DISCONNECTED"),
    USER_REQUEST(key = "USER-REQUEST"),
    DEVICE_REBOOT(key = "DEVICE-REBOOT"),
    EXPIRED_DATA(key = "EXPIRED-DATA");

    companion object {
        private val map = entries.associateBy { it.key.uppercase() }

        fun fromKey(key: String?): LastStatsResetReason? = map[key?.uppercase()]
    }
}

data class ScreenOnFields(
    val sinceBoot: Long,
    val sinceLastReset: Long
)

interface IBatteryServiceData {
    data class BatteryServiceDataSnapshot(
        val screenOnTimeSinceBoot: Long,
        val screenOnTimeSinceLastStatsReset: Long,
        val deepSleepTimeAtLastStatsReset: Long,
        val lastStatsResetTime: Long
    )

    fun getBatteryDataSnapshot(): BatteryServiceDataSnapshot
}

class NotificationService : Service() {
    companion object {
        const val CHANNEL_ID = "background-service"
        const val POWER_CONNECTED_CHANNEL_ID = "power-connected"
        const val POWER_DISCONNECTED_CHANNEL_ID = "power-disconnected"
        const val BATTERY_CHARGED_CHANNEL_ID = "battery-charged"
        const val BATTERY_LOW_CHANNEL_ID = "battery-discharged"
        const val BATTERY_TEMPERATURE_WARNING_CHANNEL_ID = "battery-temperature-warning"

        const val BATTERY_HEALTH_WARNING_CHANNEL_ID = "battery-health-warning"
        const val NOTIFICATION_ID = 1
        private const val POWER_CONNECTED_NOTIFICATION_ID = NOTIFICATION_ID + 1
        private const val POWER_DISCONNECTED_NOTIFICATION_ID = POWER_CONNECTED_NOTIFICATION_ID + 1
        private const val CHARGED_NOTIFICATION_ID = POWER_DISCONNECTED_NOTIFICATION_ID + 1
        private const val LOW_CHARGE_NOTIFICATION_ID = CHARGED_NOTIFICATION_ID + 1
        private const val TEMPERATURE_WARNING_NOTIFICATION_ID = LOW_CHARGE_NOTIFICATION_ID + 1
        private const val HEALTH_WARNING_NOTIFICATION_ID = TEMPERATURE_WARNING_NOTIFICATION_ID + 1

        private const val NOTIFICATION_CLICK_REQUEST_CODE = 1
        private const val POWER_CONNECTED_NOTIFICATION_CLICK_REQUEST_CODE = NOTIFICATION_CLICK_REQUEST_CODE + 1
        private const val POWER_DISCONNECTED_NOTIFICATION_CLICK_REQUEST_CODE = POWER_CONNECTED_NOTIFICATION_CLICK_REQUEST_CODE + 1
        private const val CHARGED_NOTIFICATION_CLICK_REQUEST_CODE = POWER_DISCONNECTED_NOTIFICATION_CLICK_REQUEST_CODE + 1
        private const val LOW_CHARGE_NOTIFICATION_CLICK_REQUEST_CODE = CHARGED_NOTIFICATION_CLICK_REQUEST_CODE + 1
        private const val CHARGED_NOTIFICATION_DELETE_REQUEST_CODE = LOW_CHARGE_NOTIFICATION_CLICK_REQUEST_CODE + 1
        private const val LOW_CHARGE_NOTIFICATION_DELETE_REQUEST_CODE = CHARGED_NOTIFICATION_DELETE_REQUEST_CODE + 1
        private const val TEMPERATURE_WARNING_NOTIFICATION_CLICK_REQUEST_CODE = LOW_CHARGE_NOTIFICATION_DELETE_REQUEST_CODE + 1
        private const val TEMPERATURE_WARNING_NOTIFICATION_DELETE_REQUEST_CODE = TEMPERATURE_WARNING_NOTIFICATION_CLICK_REQUEST_CODE + 1

        private const val HEALTH_WARNING_NOTIFICATION_CLICK_REQUEST_CODE = TEMPERATURE_WARNING_NOTIFICATION_DELETE_REQUEST_CODE + 1
        private const val POWER_CONNECTED_NOTIFICATION_TIMEOUT = 10_000L
        private const val POWER_DISCONNECTED_NOTIFICATION_TIMEOUT = POWER_CONNECTED_NOTIFICATION_TIMEOUT
        private const val UPDATE_SERVICE_NOTIFICATION_INTERVAL = 15_000L
        private const val DATA_PERSISTENCE_THRESHOLD_TOLERANCE = 600_000L
        private const val PERSISTENT_DATA_NAME = "foreground_service_data"
        const val KEY_LAST_SEEN_EVENT_TIME = "last-event-time"
        const val KEY_LAST_STATS_RESET_TIME = "last-stats-reset-time"
        const val KEY_LAST_STATS_RESET_REASON = "last-stats-reset-reason"
        const val KEY_LAST_STATS_RESET_BATTERY_LEVEL = "last-stats-reset-battery-level"
        const val KEY_DEEP_SLEEP_TIME_AT_LAST_STATS_RESET = "last-stats-reset-deep-sleep-time"
        const val KEY_TIME_SINCE_BOOT_AT_LAST_STATS_RESET = "last-stats-reset-time-since-boot"
        const val KEY_SCREEN_ON_TIME_SINCE_BOOT = "screen-on-time-since-boot"
        const val KEY_SCREEN_ON_TIME_SINCE_BOOT_IS_VALID = "screen-on-time-since-boot-is-valid"
        const val KEY_SCREEN_ON_TIME_SINCE_LAST_STATS_RESET = "screen-on-time-since-last-stats-reset"
        const val ACTION_RESET_STATS = "${BuildConfig.APPLICATION_ID}.RESET_STATS"
        const val EXTRA_REASON = "reason"
        const val ACTION_UPDATE_CHARGED_NOTIFICATION_ALARM = "${BuildConfig.APPLICATION_ID}.UPDATE_CHARGED_NOTIFICATION_ALARM"
        const val ACTION_UPDATE_BATTERY_LOW_NOTIFICATION_ALARM = "${BuildConfig.APPLICATION_ID}.UPDATE_BATTERY_LOW_NOTIFICATION_ALARM"
        const val ACTION_PERSIST_DATA_ALARM = "${BuildConfig.APPLICATION_ID}.ACTION_PERSIST_DATA_ALARM"
        private const val ACTION_CHARGED_NOTIFICATION_DELETED = "${BuildConfig.APPLICATION_ID}.CHARGED_NOTIFICATION_DELETED"
        private const val ACTION_LOW_CHARGE_NOTIFICATION_DELETED = "${BuildConfig.APPLICATION_ID}.LOW_CHARGE_NOTIFICATION_DELETED"
        private const val ACTION_TEMPERATURE_NOTIFICATION_DELETED = "${BuildConfig.APPLICATION_ID}.TEMPERATURE_NOTIFICATION_DELETED"
        private const val EXTRA_TEMPERATURE = "temperature"
        private const val SAVE_READINGS_BATTERY_LEVEL_THRESHOLD = 1
        private const val SAVE_READINGS_BATTERY_TEMPERATURE_THRESHOLD = 0.5f

        private var _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private fun getLogger(context: Context): Logger = Main.from(context).logger

        private fun getDeepSleepTime(millisSinceBoot: Long): Long = (millisSinceBoot - SystemClock.uptimeMillis()).coerceIn(0L, millisSinceBoot)

        private fun getDeepSleepTime(): Long = getDeepSleepTime(SystemClock.elapsedRealtime())

        @SuppressLint("UnsafeImplicitIntentLaunch")
        fun resetStats(context: Context, reason: LastStatsResetReason) {
            getLogger(context).log("Resetting stats data request due to ${reason.key}")

            getPersistentDataPreferences(context).edit {
                val millisSinceBoot = SystemClock.elapsedRealtime()
                val now = System.currentTimeMillis()
                val interval = if (reason == LastStatsResetReason.DEVICE_REBOOT) millisSinceBoot else 0L

                val batteryIntentHelper = run {
                    val batteryIntent = Main.from(context).batteryIntentProvider.get(true)
                    batteryIntent?.let { BatteryIntentHelper(it) }
                }

                putLong(KEY_LAST_STATS_RESET_TIME, now - interval)
                putString(KEY_LAST_STATS_RESET_REASON, reason.key)
                putLong(KEY_TIME_SINCE_BOOT_AT_LAST_STATS_RESET, millisSinceBoot - interval)
                putLong(KEY_DEEP_SLEEP_TIME_AT_LAST_STATS_RESET, getDeepSleepTime(millisSinceBoot).coerceIn(0L, millisSinceBoot - interval))
                putInt(KEY_LAST_STATS_RESET_BATTERY_LEVEL, batteryIntentHelper?.level ?: -1)

                if (reason == LastStatsResetReason.DEVICE_REBOOT || reason == LastStatsResetReason.EXPIRED_DATA) {
                    putBoolean(KEY_SCREEN_ON_TIME_SINCE_BOOT_IS_VALID, reason == LastStatsResetReason.DEVICE_REBOOT)
                    remove(KEY_SCREEN_ON_TIME_SINCE_BOOT)
                }

                remove(KEY_SCREEN_ON_TIME_SINCE_LAST_STATS_RESET)

                putLong(KEY_LAST_SEEN_EVENT_TIME, millisSinceBoot - interval)
            }
            
            context.sendBroadcast(Intent(ACTION_RESET_STATS).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_REASON, reason.key)
            })
        }

        fun resetStats(context: Context) = resetStats(context, LastStatsResetReason.USER_REQUEST)

        fun getPersistentDataPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(PERSISTENT_DATA_NAME, MODE_PRIVATE)

        fun runOrStop(context: Context, action: String?) {
            val prefs = NotificationPreferences(context)
            val willRun = prefs.isNotificationEnabled

            val intent = Intent(context, NotificationService::class.java).apply {
                action?.let { this.action = it }
            }

            if (willRun) context.startForegroundService(intent)
            else context.stopService(intent)
        }

        fun runOrStop(context: Context) = runOrStop(context, null)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            logger.log("Notification Service has received a ${action?.substringAfterLast(".")} event")

            when (action) {
                Intent.ACTION_SCREEN_ON -> {
                    onScreenTurnedOn()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    onScreenTurnedOff()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    onPowerConnected()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    onPowerDisconnected()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    Main.from(context).batteryIntentProvider.update(intent)
                    onBatteryChanged(intent)
                }
                ACTION_RESET_STATS -> {
                    val reason = intent.getStringExtra(EXTRA_REASON)

                    if ((reason != null) && (reason != LastStatsResetReason.EXPIRED_DATA.key)) loadPersistedDataOrReset()
                }
                ACTION_CHARGED_NOTIFICATION_DELETED -> {
                    onChargedNotificationDeleted()
                }
                ACTION_LOW_CHARGE_NOTIFICATION_DELETED -> {
                    onBatteryLowChargedNotificationDeleted()
                }
                ACTION_TEMPERATURE_NOTIFICATION_DELETED -> {
                    onTemperatureNotificationDeleted(intent)
                }
            }
        }
    }

    private val logger by lazy { Main.from(this).logger }
    private val prefs by lazy { NotificationPreferences(this) }
    private val appPrefs by lazy { AppPreferences(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val repository by lazy { BatteryRepository(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val updateNotificationTask by lazy {
        RepeatingTask(handler, "Update Notification Task", logger) {
            val notification = buildServiceNotification()

            if (applicationContext.hasPostNotificationsPermission())
                @SuppressLint("MissingPermission")
                NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }
    private val persistentData by lazy { getPersistentDataPreferences(this) }

    private val notificationFieldFormats = mutableMapOf<NotificationBatteryIntentHelper.NotificationField, String>()
    private var notificationFieldFormatsLocale: LocaleList? = null

    private val configCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            if (newConfig.locales != notificationFieldFormatsLocale) {
                notificationFieldFormatsLocale = newConfig.locales
                notificationFieldFormats.clear()
           }
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {}
    }

    private fun getCachedLabel(notificationField: NotificationBatteryIntentHelper.NotificationField): String =
        notificationFieldFormats.getOrPut(notificationField) {
            getString(R.string.label_and_value,
            notificationField.getLabel(this, true),
                "%s"
            )
        }

    private var serviceStartTime = 0L
    private var disablePersistData = false

    private var lastStatsResetTime = 0L
    private var lastStatsResetReason: LastStatsResetReason? = null
    private var deepSleepTimeAtLastStatsReset = 0L

    private var lastBatteryEventTime = 0L
    private var lastChargingStartLevel = 0

    private var screenOnTimeSinceBootIsValid = false
    private var screenOnTimeSinceBoot = 0L

    private var screenOnTimeSinceLastStatsReset = 0L

    private var isScreenOn = false
    private var lastScreenEventTime = 0L

    private var batteryCharged = false

    private var batteryLow = false

    private var batteryTemperatureNotifiedLevel = -1f
    private var batteryTemperatureNotificationDeletionLevel = -1

    private var lastSavedBatteryLevel = -1

    private var lastSavedBatteryFullCharged = false
    private var lastSavedTemperature = -1f
    private var lastSavedIsCharging = false

    private var healthNotificationShown = false

    private val binder = object : android.os.Binder(), IBatteryServiceData {
        override fun getBatteryDataSnapshot(): IBatteryServiceData.BatteryServiceDataSnapshot {
            return IBatteryServiceData.BatteryServiceDataSnapshot(
                screenOnTimeSinceBoot = if (this@NotificationService.screenOnTimeSinceBootIsValid) this@NotificationService.screenOnTimeSinceBoot else -1L,
                screenOnTimeSinceLastStatsReset = this@NotificationService.screenOnTimeSinceLastStatsReset,
                deepSleepTimeAtLastStatsReset = this@NotificationService.deepSleepTimeAtLastStatsReset,
                lastStatsResetTime = this@NotificationService.lastStatsResetTime
            )
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        _isRunning.value = true

        logger.log("Notification Service was created")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(ACTION_RESET_STATS)
            addAction(ACTION_CHARGED_NOTIFICATION_DELETED)
            addAction(ACTION_LOW_CHARGE_NOTIFICATION_DELETED)
            addAction(ACTION_TEMPERATURE_NOTIFICATION_DELETED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }

        notificationFieldFormatsLocale = resources.configuration.locales
        registerComponentCallbacks(configCallbacks)
    }

    override fun onDestroy() {
        _isRunning.value = false

        logger.log("Notification Service was destroyed")

        unregisterComponentCallbacks(configCallbacks)

        unregisterReceiver(receiver)

        AlarmsReceiver.stop(this, AlarmsReceiver.Tags.NOTIFICATION_SERVICE_PERSIST_DATA)

        onScreenTurnedOff()

        hideChargedNotification()

        hideBatteryLowChargedNotification()

        hideTemperatureNotification()

        super.onDestroy()
    }

    private fun loadPersistedDataOrReset() {
        val now = System.currentTimeMillis()
        val millisSinceBoot = SystemClock.elapsedRealtime()

        val lastSeenEventTime = persistentData.getLong(KEY_LAST_SEEN_EVENT_TIME, 0L)
        val intervalWithoutEvents = millisSinceBoot - lastSeenEventTime

        lastStatsResetTime = now
        lastStatsResetReason = LastStatsResetReason.EXPIRED_DATA
        deepSleepTimeAtLastStatsReset = getDeepSleepTime()
        screenOnTimeSinceBootIsValid = false
        screenOnTimeSinceBoot = 0L
        screenOnTimeSinceLastStatsReset = 0L

        if (intervalWithoutEvents in 0..DATA_PERSISTENCE_THRESHOLD_TOLERANCE) {
            logger.log("Loading persisted data")

            lastStatsResetTime = persistentData.getLong(KEY_LAST_STATS_RESET_TIME, lastStatsResetTime)
            lastStatsResetReason = LastStatsResetReason.fromKey(persistentData.getString(KEY_LAST_STATS_RESET_REASON, null))
            deepSleepTimeAtLastStatsReset = persistentData.getLong(KEY_DEEP_SLEEP_TIME_AT_LAST_STATS_RESET, deepSleepTimeAtLastStatsReset)
            screenOnTimeSinceBootIsValid = persistentData.getBoolean(KEY_SCREEN_ON_TIME_SINCE_BOOT_IS_VALID, screenOnTimeSinceBootIsValid)
            screenOnTimeSinceBoot = persistentData.getLong(KEY_SCREEN_ON_TIME_SINCE_BOOT, screenOnTimeSinceBoot)
            screenOnTimeSinceLastStatsReset = persistentData.getLong(KEY_SCREEN_ON_TIME_SINCE_LAST_STATS_RESET, screenOnTimeSinceLastStatsReset)
        } else {
            resetStats(LastStatsResetReason.EXPIRED_DATA)
        }

        disablePersistData = false

        lastScreenEventTime = millisSinceBoot
        isScreenOn = isScreenOn()
    }

    private fun initializeService() {
        val now = System.currentTimeMillis()

        serviceStartTime = now

        loadPersistedDataOrReset()

        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)

        if (isScreenOn) onScreenTurnedOn()

        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)
        val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(it) }

        if (batteryIntentHelper != null) {
            val millisSinceBoot = SystemClock.elapsedRealtime()

            lastBatteryEventTime = millisSinceBoot
            lastChargingStartLevel = if (batteryIntentHelper.isCharging) batteryIntentHelper.level else -1
        }
    }

    private fun persistData() {
        if (!disablePersistData) {
            val screenOnFields = getScreenOnTime()
            val millisSinceBoot = SystemClock.elapsedRealtime()

            screenOnTimeSinceBoot = screenOnFields.sinceBoot
            screenOnTimeSinceLastStatsReset = screenOnFields.sinceLastReset
            lastScreenEventTime = millisSinceBoot

            logger.log("Persisting data")

            persistentData.edit {
                putLong(KEY_SCREEN_ON_TIME_SINCE_BOOT, screenOnTimeSinceBoot)
                putLong(KEY_SCREEN_ON_TIME_SINCE_LAST_STATS_RESET, screenOnTimeSinceLastStatsReset)

                putLong(KEY_LAST_SEEN_EVENT_TIME, lastScreenEventTime)
            }
        }
    }

    private fun resetStats(reason: LastStatsResetReason) {
        disablePersistData = true

        logger.log("Persisted data will be reset due to ${reason.key}")

        resetStats(this, reason)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val serviceInitialized = (serviceStartTime != 0L)

        if ((!serviceInitialized) || (!action.isNullOrEmpty())) {
            logger.log("Notification Service started with '${action?.substringAfterLast(".")}' modifier (service is ${if (serviceInitialized) "already" else "not"} running)")

            if (!serviceInitialized) initializeService()
            persistData()

            if (action == ACTION_UPDATE_CHARGED_NOTIFICATION_ALARM) showOrHideChargedNotification()
            else if (action == ACTION_UPDATE_BATTERY_LOW_NOTIFICATION_ALARM) showOrHideBatteryLowChargedNotification()

            AlarmsReceiver.start(
                this,
                AlarmsReceiver.Tags.NOTIFICATION_SERVICE_PERSIST_DATA,
                (0.75f * DATA_PERSISTENCE_THRESHOLD_TOLERANCE).toLong()
            )
        }

        return START_STICKY
    }

    fun getScreenOnTime(): ScreenOnFields {
        val millisSinceBoot = SystemClock.elapsedRealtime()
        val interval = if (isScreenOn) millisSinceBoot - lastScreenEventTime else 0

        return ScreenOnFields(screenOnTimeSinceBoot + interval, screenOnTimeSinceLastStatsReset + interval)
    }

    private fun onScreenTurnedOn() {
        persistData()

        isScreenOn = true

        saveScreenStateToDB()

        updateNotificationTask.startRepeating(0L, UPDATE_SERVICE_NOTIFICATION_INTERVAL)
    }
    private fun onScreenTurnedOff() {
        persistData()

        isScreenOn = false

        saveScreenStateToDB()

        updateNotificationTask.stop()
    }

    private fun saveScreenStateToDB() {
        serviceScope.launch {
            val now = System.currentTimeMillis()

            repository.insertScreenState(ScreenState(timestamp = now, screenOn = isScreenOn))

            val batteryHistoryWindowInMillis = (appPrefs.batteryHistoryWindow + 1) * 24 * 60 * 60 * 1_000L
            val batteryScreenHistoryCutOffTime = now - batteryHistoryWindowInMillis
            repository.deleteScreenStatesOlderThan(batteryScreenHistoryCutOffTime)
        }
    }

    private fun onPowerConnected() {
        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)
        val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(it) }
        val millisSinceBoot = SystemClock.elapsedRealtime()

        lastBatteryEventTime = millisSinceBoot
        lastChargingStartLevel = batteryIntentHelper?.level ?: -1

        persistData()

        showPowerConnectedNotification(batteryIntentHelper)

        hideBatteryLowChargedNotification()
    }

    private fun onPowerDisconnected() {
        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)
        val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(it) }
        val millisSinceBoot = SystemClock.elapsedRealtime()

        if ((! batteryCharged) || (!prefs.isBlockingPowerDisconnectNotificationWhenBatteryIsCharged))
            showPowerDisconnectedNotification(batteryIntentHelper)

        val shouldReset = (
            prefs.isAutoResetStatsEnabled &&
            (batteryIntentHelper != null) &&
            (
                batteryIntentHelper.isFullCharged ||
                (
                    (
                        (lastChargingStartLevel >= 0) &&
                        (batteryIntentHelper.level >= (lastChargingStartLevel + prefs.resetStatsBatteryThresholdPercent).coerceAtMost(100))
                    ) &&
                    (lastBatteryEventTime + prefs.resetStatsBatteryChargeTime * 60_000L < millisSinceBoot)
                )
            )
        )

        if (shouldReset) resetStats(LastStatsResetReason.POWER_DISCONNECTED)
        else persistData()

        lastBatteryEventTime = millisSinceBoot

        hideChargedNotification()
    }

    private fun isBatteryCharged(batteryIntentHelper: BatteryIntentHelper): Boolean {
        val isCharging = batteryIntentHelper.isCharging
        val isFullCharged = batteryIntentHelper.isFullCharged
        val level = batteryIntentHelper.level

        return isCharging && (isFullCharged || (level >= prefs.notificationChargedPercent))
    }

    private fun isBatteryLow(batteryIntentHelper: BatteryIntentHelper): Boolean {
        val isCharging = batteryIntentHelper.isCharging
        val level = batteryIntentHelper.level

        return (!isCharging) && (level <= prefs.notificationLowChargePercent)
    }

    private fun onBatteryChanged(intent: Intent) {
        val batteryIntentHelper = BatteryIntentHelper(intent)

        val status = batteryIntentHelper.toString(this, BatteryIntentHelper.BATTERY_STATUS, appPrefs, true)
        val level = batteryIntentHelper.toString(this, BatteryIntentHelper.BATTERY_LEVEL, appPrefs, true)
        val temperature = batteryIntentHelper.toString(this, BatteryIntentHelper.BATTERY_TEMPERATURE, appPrefs, true)

        logger.log("Current battery relevant values: status=$status, level=$level, temperature=$temperature")

        persistData()

        if ((!batteryCharged) && isBatteryCharged(batteryIntentHelper)) {
            logger.log("Battery charged notification will be shown")

            batteryCharged = true

            val batteryChargedNotificationInterval = prefs.notificationChargedInterval * 60_000L
            scheduleChargedNotificationAlarm(batteryChargedNotificationInterval)
        }

        if ((!batteryLow) && isBatteryLow(batteryIntentHelper)) {
            logger.log("Battery low notification will be shown")

            batteryLow = true

            showOrHideBatteryLowChargedNotification()
        }

        val temperatureThreshold = prefs.getBatteryTemperatureThreshold(TemperatureUnit.CELSIUS)
        val shouldTemperatureBeNotified = batteryIntentHelper.temperatureCelsius >= temperatureThreshold
        val shouldTemperatureBeNotifiedAgain = when {
            batteryTemperatureNotifiedLevel == -1f -> true
            batteryTemperatureNotificationDeletionLevel != -1 -> batteryIntentHelper.temperatureCelsius > batteryTemperatureNotificationDeletionLevel
            else -> batteryTemperatureNotifiedLevel != batteryIntentHelper.temperatureCelsius
        }
        if (shouldTemperatureBeNotified && shouldTemperatureBeNotifiedAgain) {
            logger.log("Temperature Alert notification will be shown")
            batteryTemperatureNotifiedLevel = batteryIntentHelper.temperatureCelsius
            showTemperatureNotification(batteryIntentHelper)
        } else if (!shouldTemperatureBeNotified && (batteryTemperatureNotifiedLevel != -1f)) {
            logger.log("Temperature Alert notification will be hidden")
            batteryTemperatureNotifiedLevel = -1f
            batteryTemperatureNotificationDeletionLevel = -1
            hideTemperatureNotification()
        }

        val shouldHealthBeNotified = batteryIntentHelper.health !in listOf(BatteryManager.BATTERY_HEALTH_GOOD, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val shouldHealthBeNotifiedAgain = !healthNotificationShown
        if (shouldHealthBeNotified && shouldHealthBeNotifiedAgain) {
            logger.log("Health Alert notification will be shown")
            healthNotificationShown = true
            showHealthNotification(batteryIntentHelper)
        } else if (!shouldHealthBeNotified && healthNotificationShown) {
            logger.log("Health Alert notification will be hidden")
            hideHealthNotification()
            healthNotificationShown = false
        }

        saveBatteryDataToDB(batteryIntentHelper)
    }

    private fun saveBatteryDataToDB(batteryIntentHelper: BatteryIntentHelper) {
        val now = System.currentTimeMillis()
        val level = batteryIntentHelper.level
        val temperature = batteryIntentHelper.temperatureCelsius
        val isCharging = batteryIntentHelper.isCharging
        val isFullCharged = batteryIntentHelper.isFullCharged

        val levelChanged = if (lastSavedBatteryLevel < 0) { level >= 0 } else { abs(level - lastSavedBatteryLevel) >= SAVE_READINGS_BATTERY_LEVEL_THRESHOLD }
        val fullChargedChanged = lastSavedBatteryFullCharged != isFullCharged
        val tempChanged = if (lastSavedTemperature < 0f) { temperature >= 0f } else { abs(temperature - lastSavedTemperature) >= SAVE_READINGS_BATTERY_TEMPERATURE_THRESHOLD }
        val chargingChanged = isCharging != lastSavedIsCharging

        if ((!levelChanged) && (!fullChargedChanged) && (!tempChanged) && (!chargingChanged)) return

        lastSavedBatteryLevel = level
        lastSavedBatteryFullCharged = isFullCharged
        lastSavedTemperature = temperature
        lastSavedIsCharging = isCharging

        serviceScope.launch {
            try {
                val reading = BatteryReading(
                    timestamp = now,
                    batteryLevel = level,
                    batteryStatus = batteryIntentHelper.status,
                    temperatureCelsius = temperature,
                    voltage = batteryIntentHelper.voltage,
                    health = batteryIntentHelper.health,
                    isCharging = isCharging,
                    plugType = batteryIntentHelper.plugType
                )
                repository.insertBatteryReading(reading)

                val batteryReadingsHistoryWindowInMillis = appPrefs.batteryHistoryWindow * 24 * 60 * 60 * 1_000L
                val batteryReadingsHistoryCutOffTime = now - batteryReadingsHistoryWindowInMillis
                repository.deleteBatteryReadingsOlderThan(batteryReadingsHistoryCutOffTime)

                logger.log("Battery reading stored at DB")

                var currentChargeSession: ChargingSession? = null

                if (isFullCharged) {
                    currentChargeSession = repository.getOpenChargingSession()
                    if (currentChargeSession != null && currentChargeSession.chargedTimeStamp == null) {
                        repository.updateChargingSession(currentChargeSession.copy(chargedTimeStamp = now))
                        logger.log("Battery full Charged timestamp recorded at DB")
                    }
                }

                if (chargingChanged) {
                    if (isCharging) {
                        val chargeSession = ChargingSession(
                            startTime = now,
                            endTime = null,
                            startLevel = level,
                            endLevel = null,
                            plugType = batteryIntentHelper.plugType,
                            durationMinutes = null,
                            avgTemperatureCelsius = null,
                            maxTemperatureCelsius = null,
                            minTemperatureCelsius = null,
                            chargedTimeStamp = null
                        )
                        repository.insertChargingSession(chargeSession)
                        logger.log("Charging session start stored at DB")
                    } else {
                        currentChargeSession = currentChargeSession ?: repository.getOpenChargingSession()

                        if (currentChargeSession != null) {
                            val readings = repository.getBatteryReadingsBetween(currentChargeSession.startTime, now)
                            val temps = readings.map { it.temperatureCelsius }.filter { it >= 0f }
                            val durationMs = now - currentChargeSession.startTime

                            if (durationMs > batteryReadingsHistoryWindowInMillis) {
                                repository.deleteChargingSession(currentChargeSession.id)
                                logger.log("Charging session discarded from DB due to exceeds readings window")
                            } else {
                                val updatedChargeSession = currentChargeSession.copy(
                                    endTime = now,
                                    endLevel = level,
                                    durationMinutes = durationMs / 60_000L,
                                    avgTemperatureCelsius = if (temps.isNotEmpty()) temps.average().toFloat() else null,
                                    maxTemperatureCelsius = if (temps.isNotEmpty()) temps.max() else null,
                                    minTemperatureCelsius = if (temps.isNotEmpty()) temps.min() else null
                                )
                                repository.updateChargingSession(updatedChargeSession)
                                logger.log("Charging session end stored at DB (${updatedChargeSession.durationMinutes} min, ${currentChargeSession.startLevel}% → ${level}%)")
                            }
                        }
                    }
                }

                val chargingHistoryWindowInMillis = appPrefs.chargingHistoryWindow * 24 * 60 * 60 * 1_000L
                val chargingHistoryCutOffTime = now - chargingHistoryWindowInMillis
                repository.deleteChargingSessionsOlderThan(chargingHistoryCutOffTime)
            } catch (e: Exception) {
                logger.log("Error saving data to DB: ${e.message}")
            }
        }
    }

    private fun buildServiceNotification(): Notification {
        fun buildText(notificationBatteryIntentHelper: NotificationBatteryIntentHelper): String {
            val values = NotificationBatteryIntentHelper.NotificationField.entries.associateWith { field ->
                notificationBatteryIntentHelper.toString(this, field, prefs, appPrefs)
            }

            val fields = NotificationBatteryIntentHelper.NotificationField.entries
                .filter {
                    notificationBatteryIntentHelper.isValid(it) && notificationBatteryIntentHelper.isVisible(it, prefs) && prefs.isFieldVisible(it)
                }
                .map { field ->
                    field to getCachedLabel(field).format(values[field])
                }
                .sortedBy { (field, _) ->
                    prefs.getFieldPosition(field)
                }

            return fields.joinToString("\n") { it.second }
        }

        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)

        val text = if (batteryIntent == null) {
            getString(R.string.data_is_not_available)
        }
        else {
            val screenOnFields = getScreenOnTime()
            val notificationBatteryIntentHelper = NotificationBatteryIntentHelper(
                batteryIntent,
                lastStatsResetTime,
                deepSleepTimeAtLastStatsReset,
                if (screenOnTimeSinceBootIsValid) screenOnFields.sinceBoot else -1L,
                screenOnFields.sinceLastReset,
                lastBatteryEventTime)

            buildText(notificationBatteryIntentHelper)
        }

        val whatAppOpens = appPrefs.whatAppOpensWhenUserClicksTileOrNotification
        val clickIntent = whatAppOpens.getIntent(this)

        return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setSmallIcon(R.drawable.ic_statusbar_notification)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setWhen(serviceStartTime)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                NOTIFICATION_CLICK_REQUEST_CODE,
                clickIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        icon: Int,
        clickRequestCode: Int,
        logMessagePrefix: String,
        timeoutAfter: Long? = null,
        onlyAlertOnce: Boolean = true,
        deleteIntent: Intent? = null,
        deleteRequestCode: Int = 0
    ) {
        if (!hasPostNotificationsPermission()) {
            logger.log("$logMessagePrefix notification hasn't posted due to lack of permissions")
            return
        }

        val clickIntent = WhatAppOpens.APP.getIntent(this)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    clickRequestCode,
                    clickIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        if (timeoutAfter != null) builder.setTimeoutAfter(timeoutAfter)

        if (deleteIntent != null) {
            builder.setDeleteIntent(
                PendingIntent.getBroadcast(
                    this,
                    deleteRequestCode,
                    deleteIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val notification = builder.build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)

        logger.log("$logMessagePrefix notification has been posted")
    }

    @SuppressLint("MissingPermission")
    private fun showPowerConnectedNotification(batteryIntentHelper: BatteryIntentHelper?) {
        val level = batteryIntentHelper?.level ?: -1

        val title = if (level < 0) getString(R.string.power_connected_notification_title)
                   else getString(R.string.power_connected_notification_title_with_charge_value, level)

        postNotification(
            channelId = POWER_CONNECTED_CHANNEL_ID,
            notificationId = POWER_CONNECTED_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_power_connected,
            clickRequestCode = POWER_CONNECTED_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Charging",
            timeoutAfter = POWER_CONNECTED_NOTIFICATION_TIMEOUT
        )
    }

    @SuppressLint("MissingPermission")
    private fun showPowerDisconnectedNotification(batteryIntentHelper: BatteryIntentHelper?) {
        val level = batteryIntentHelper?.level ?: -1

        val title = if (level < 0) getString(R.string.power_disconnected_notification_title)
                   else getString(R.string.power_disconnected_notification_title_with_charge_value, level)

        postNotification(
            channelId = POWER_DISCONNECTED_CHANNEL_ID,
            notificationId = POWER_DISCONNECTED_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_power_disconnected,
            clickRequestCode = POWER_DISCONNECTED_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Discharging",
            timeoutAfter = POWER_DISCONNECTED_NOTIFICATION_TIMEOUT
        )
    }

    @SuppressLint("MissingPermission")
    private fun showTemperatureNotification(batteryIntentHelper: BatteryIntentHelper) {
        val title = getString(R.string.temperature_alert_with_temperature_value, batteryIntentHelper.toString(this, BatteryIntentHelper.BATTERY_TEMPERATURE, appPrefs, false))

        val deleteIntent = Intent(ACTION_TEMPERATURE_NOTIFICATION_DELETED).apply {
            putExtra(EXTRA_TEMPERATURE, batteryIntentHelper.temperatureCelsius)
            setPackage(packageName)
        }

        postNotification(
            channelId = BATTERY_TEMPERATURE_WARNING_CHANNEL_ID,
            notificationId = TEMPERATURE_WARNING_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_battery_temperature,
            clickRequestCode = TEMPERATURE_WARNING_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Temperature Warning",
            onlyAlertOnce = false,
            deleteIntent = deleteIntent,
            deleteRequestCode = TEMPERATURE_WARNING_NOTIFICATION_DELETE_REQUEST_CODE
        )
    }

    private fun hideTemperatureNotification() =
        NotificationManagerCompat.from(this).cancel(TEMPERATURE_WARNING_NOTIFICATION_ID)

    private fun onTemperatureNotificationDeleted(intent: Intent) {
        batteryTemperatureNotificationDeletionLevel = intent.getIntExtra(EXTRA_TEMPERATURE, -1)
    }

    @SuppressLint("MissingPermission")
    private fun showHealthNotification(batteryIntentHelper: BatteryIntentHelper) {
        val title = getString(R.string.health_alert_with_value, batteryIntentHelper.toString(this, BatteryIntentHelper.BATTERY_HEALTH, appPrefs, false))

        postNotification(
            channelId = BATTERY_HEALTH_WARNING_CHANNEL_ID,
            notificationId = HEALTH_WARNING_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_battery_health,
            clickRequestCode = HEALTH_WARNING_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Health Warning",
            onlyAlertOnce = false
        )
    }

    private fun hideHealthNotification() =
        NotificationManagerCompat.from(this).cancel(HEALTH_WARNING_NOTIFICATION_ID)

    @SuppressLint("MissingPermission")
    private fun showChargedNotification(batteryIntentHelper: BatteryIntentHelper) {
        val title = getString(R.string.battery_charged_with_charge_value, batteryIntentHelper.level)

        val deleteIntent = Intent(ACTION_CHARGED_NOTIFICATION_DELETED).setPackage(packageName)

        postNotification(
            channelId = BATTERY_CHARGED_CHANNEL_ID,
            notificationId = CHARGED_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_battery_charged,
            clickRequestCode = CHARGED_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Charged",
            onlyAlertOnce = false,
            deleteIntent = deleteIntent,
            deleteRequestCode = CHARGED_NOTIFICATION_DELETE_REQUEST_CODE
        )
    }

    private fun hideChargedNotification() {
        if (batteryCharged) {
            batteryCharged = false

            cancelChargedNotificationAlarm()

            NotificationManagerCompat.from(this).cancel(CHARGED_NOTIFICATION_ID)
        }
    }

    private fun showOrHideChargedNotification() {
        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)
        val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(it) }

        logger.log("Received charged notification redraw alarm")

        val repeatInterval = prefs.notificationChargedRepeatInterval
        val shouldScheduleAlarm = repeatInterval != 0
        val delay = repeatInterval * 60_000L

        when {
            batteryIntentHelper == null -> {
                logger.log("Bypassing charged notification post due to battery intent isn't available")

                if (shouldScheduleAlarm) scheduleChargedNotificationAlarm(delay)
            }

            batteryCharged && isBatteryCharged(batteryIntentHelper) -> {
                logger.log("Posting charged notification due to battery is still charged")

                showChargedNotification(batteryIntentHelper)

                if (shouldScheduleAlarm) scheduleChargedNotificationAlarm(delay)
            }

            else -> {
                logger.log("Hiding charged notification due to battery is not charged")
                hideChargedNotification()
            }
        }
    }

    private fun scheduleChargedNotificationAlarm(delay: Long) {
        val now = System.currentTimeMillis()
        logger.log("Scheduling charged alarm notification redraw in ${delay / 1000} seconds, at $TIME_REFERENCE", now + delay)

        AlarmsReceiver.start(this, AlarmsReceiver.Tags.NOTIFICATION_SERVICE_UPDATE_CHARGED_NOTIFICATION, delay)
    }

    private fun cancelChargedNotificationAlarm() {
        logger.log("Cancelling charged notification redraw alarm")

        AlarmsReceiver.stop(this, AlarmsReceiver.Tags.NOTIFICATION_SERVICE_UPDATE_CHARGED_NOTIFICATION)
    }

    private fun onChargedNotificationDeleted() = cancelChargedNotificationAlarm()

    @SuppressLint("MissingPermission")
    private fun showBatteryLowChargedNotification(batteryIntentHelper: BatteryIntentHelper) {
        val title = getString(R.string.battery_low_with_charge_value, batteryIntentHelper.level)

        val deleteIntent = Intent(ACTION_LOW_CHARGE_NOTIFICATION_DELETED).setPackage(packageName)

        postNotification(
            channelId = BATTERY_LOW_CHANNEL_ID,
            notificationId = LOW_CHARGE_NOTIFICATION_ID,
            title = title,
            icon = R.drawable.ic_statusbar_notification_battery_low,
            clickRequestCode = LOW_CHARGE_NOTIFICATION_CLICK_REQUEST_CODE,
            logMessagePrefix = "Battery low",
            onlyAlertOnce = false,
            deleteIntent = deleteIntent,
            deleteRequestCode = LOW_CHARGE_NOTIFICATION_DELETE_REQUEST_CODE
        )
    }

    private fun hideBatteryLowChargedNotification() {
        if (batteryLow) {
            batteryLow = false

            cancelBatteryLowChargedNotificationAlarm()

            NotificationManagerCompat.from(this).cancel(LOW_CHARGE_NOTIFICATION_ID)
        }
    }

    private fun showOrHideBatteryLowChargedNotification() {
        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)
        val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(it) }

        logger.log("Received battery low notification redraw alarm")

        val repeatInterval = prefs.notificationLowChargeRepeatInterval
        val shouldScheduleAlarm = repeatInterval != 0
        val delay = repeatInterval * 60_000L

        when {
            batteryIntentHelper == null -> {
                logger.log("Bypassing battery low notification post due to battery intent isn't available")

                if (shouldScheduleAlarm) scheduleBatteryLowChargedNotificationAlarm(delay)
            }

            batteryLow && isBatteryLow(batteryIntentHelper) -> {
                logger.log("Posting battery low notification due to battery charge is still low")

                showBatteryLowChargedNotification(batteryIntentHelper)

                if (shouldScheduleAlarm) scheduleBatteryLowChargedNotificationAlarm(delay)
            }

            else -> {
                logger.log("Hiding battery low notification due to battery charge is not low")
                hideBatteryLowChargedNotification()
            }
        }
    }

    private fun scheduleBatteryLowChargedNotificationAlarm(delay: Long) {
        val now = System.currentTimeMillis()
        logger.log("Scheduling battery low alarm notification redraw in ${delay / 1000} seconds, at $TIME_REFERENCE", now + delay)

        AlarmsReceiver.start(this, AlarmsReceiver.Tags.NOTIFICATION_SERVICE_UPDATE_BATTERY_LOW_NOTIFICATION, delay)
    }

    private fun cancelBatteryLowChargedNotificationAlarm() {
        logger.log("Cancelling battery low notification redraw alarm")

        AlarmsReceiver.stop(this, AlarmsReceiver.Tags.NOTIFICATION_SERVICE_UPDATE_BATTERY_LOW_NOTIFICATION)
    }

    private fun onBatteryLowChargedNotificationDeleted() = cancelBatteryLowChargedNotificationAlarm()












}
