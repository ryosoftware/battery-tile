package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ryosoftware.battery_tile.WhatAppOpens.Companion.getIntent

class BatteryTileService : TileService() {
    companion object {
        private const val REQUEST_CODE = 101

        private const val UPDATE_TILE_INTERVAL = 15_000L
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            logger.log("Tile Service has received a ${action?.substringAfterLast(".")} event")

            if (action == Intent.ACTION_BATTERY_CHANGED) {
                Main.from(context).batteryIntentProvider.update(intent)
                updateTile(intent)
            }
        }
    }

    private val logger by lazy { Main.from(this).logger }

    private val handler = Handler(Looper.getMainLooper())

    private val updateTileTask by lazy {
        RepeatingTask(handler, "Update Tile Task", logger) {
            updateTile()
        }
    }

    private val prefs by lazy { BatteryTilePreferences(this) }
    private val appPrefs by lazy { AppPreferences(this) }

    private var batteryService: IBatteryServiceData? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            batteryService = (binder as IBatteryServiceData)
            logger.log("Tile bound to Notification Service")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            batteryService = null
            logger.log("Tile unbound from Notification Service")
        }
    }

    override fun onStartListening() {
        logger.log("Tile Service start listening")

        super.onStartListening()

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }

        bindService(Intent(this, NotificationService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        updateTileTask.startRepeating(0L, UPDATE_TILE_INTERVAL)

    }

    override fun onStopListening() {
        logger.log("Tile Service stop listening")

        unregisterReceiver(receiver)

        updateTileTask.stop()

        unbindService(serviceConnection)
        batteryService = null

        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val whatAppOpens = appPrefs.whatAppOpensWhenUserClicksTileOrNotification
        val intent = whatAppOpens.getIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(
                intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun updateTile(batteryIntent: Intent) {
        fun buildText(batteryTileBatteryIntentHelper: BatteryTileBatteryIntentHelper): Pair<String, String> {
            val separator = getString(R.string.tile_separator)

            val (lines1, lines2) = BatteryTileBatteryIntentHelper.BatteryTileField.entries
                .asSequence()
                .filter {
                    it.textualizable && batteryTileBatteryIntentHelper.isValid(it) && prefs.isFieldVisible(it)
                }
                .sortedBy { prefs.getFieldPosition(it) }
                .map { field ->
                    val line = prefs.getFieldLine(field)
                    val text = batteryTileBatteryIntentHelper.toString(this, field, appPrefs)
                    line to text
                }
                .partition { (line, _) -> line == 1 }
                .let { (first, second) ->
                    first.map { it.second } to second.map { it.second }
                }

            return lines1.joinToString(separator) to lines2.joinToString(separator)
        }

        val tile = qsTile ?: return

        val batteryServiceDataSnapshot = batteryService?.getBatteryDataSnapshot()
        val batteryTileBatteryIntentHelper = BatteryTileBatteryIntentHelper(batteryIntent,
            batteryServiceDataSnapshot?.lastStatsResetTime ?: -1L,
            batteryServiceDataSnapshot?.deepSleepTimeAtLastStatsReset ?: -1L,
            batteryServiceDataSnapshot?.screenOnTimeSinceBoot ?: -1L,
            batteryServiceDataSnapshot?.screenOnTimeSinceLastStatsReset ?: -1L)
        val (line1, line2) = buildText(batteryTileBatteryIntentHelper)

        tile.state = if (batteryTileBatteryIntentHelper.isCharging) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = batteryTileBatteryIntentHelper.getIcon(this, prefs.iconField, appPrefs)
        tile.label = line1
        tile.subtitle = line2
        tile.updateTile()
        logger.log("Tile has been updated")

    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val batteryIntent = Main.from(this).batteryIntentProvider.get(true)

        if (batteryIntent == null) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_unknown)
            tile.label = getString(R.string.app_name)
            tile.subtitle = getString(R.string.data_is_not_available)
            tile.updateTile()
            return
        }

        updateTile(batteryIntent)
    }
}
