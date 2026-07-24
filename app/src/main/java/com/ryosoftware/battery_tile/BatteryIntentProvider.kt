package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.text.format.DateUtils

interface BatteryIntentProvider {
    fun get(useFreshValue: Boolean): Intent?

    fun get(): Intent? = get(false)
    fun update(intent: Intent)
}

class CachedBatteryIntentProvider(context: Context) : BatteryIntentProvider {
    private val applicationContext = context.applicationContext
    private var cachedIntent: Intent? = null
    private var cachedTime = 0L

    companion object {
        private const val CACHE_DURATION = 5 * DateUtils.SECOND_IN_MILLIS
    }

    override fun get(useFreshValue: Boolean): Intent? {
        val now = SystemClock.elapsedRealtime()
        if (useFreshValue || (cachedIntent == null) || (now - cachedTime > CACHE_DURATION)) {
            cachedIntent = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            cachedTime = now
        }
        return cachedIntent
    }

    override fun update(intent: Intent) {
        cachedIntent = intent
        cachedTime = SystemClock.elapsedRealtime()
    }
}