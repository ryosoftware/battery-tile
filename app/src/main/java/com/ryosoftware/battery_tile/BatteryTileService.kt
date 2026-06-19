package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ryosoftware.battery_tile.WhatAppOpens.Companion.getIntent

class BatteryTileService : TileService() {
    private val REQUEST_CODE = 101

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
    private val prefs by lazy { BatteryTilePreferences(this) }
    private val appPrefs by lazy { AppPreferences(this) }

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

        updateTile()
    }

    override fun onStopListening() {
        logger.log("Tile Service stop listening")

        unregisterReceiver(receiver)

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
        val batteryTileBatteryIntentHelper = BatteryTileBatteryIntentHelper(batteryIntent)
        val (line1, line2) = buildText(batteryTileBatteryIntentHelper)

        tile.state = if (batteryTileBatteryIntentHelper.isCharging) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = batteryTileBatteryIntentHelper.getIcon(this, prefs.iconField, appPrefs)
        tile.label = line1
        tile.subtitle = line2
        tile.updateTile()
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
