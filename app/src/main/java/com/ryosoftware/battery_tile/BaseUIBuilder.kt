package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.SystemClock
import androidx.annotation.StringRes
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercent

abstract class BaseUIBuilder(
    context: Context,
    intent: Intent,
    protected val lastStatsResetTime: Long,
    protected val deepSleepTimeAtLastStatsReset: Long,
    protected val screenOnTimeSinceBoot: Long,
    protected val screenOnTimeSinceLastStatsReset: Long,
    batteryManager: BatteryManager?
) : BatteryIntentHelper(context, intent, batteryManager) {
    val now: Long by lazy { System.currentTimeMillis() }
    val timeSinceBoot: Long by lazy { SystemClock.elapsedRealtime() }
    val deepSleepTimeSinceBoot: Long by lazy { timeSinceBoot - SystemClock.uptimeMillis() }
    val timeSinceLastStatsReset: Long by lazy { now - lastStatsResetTime }
    val deepSleepTimeSinceLastStatsReset: Long by lazy { deepSleepTimeSinceBoot - deepSleepTimeAtLastStatsReset }
}