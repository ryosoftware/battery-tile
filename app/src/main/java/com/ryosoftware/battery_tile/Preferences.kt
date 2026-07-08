package com.ryosoftware.battery_tile

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

open class Preferences(protected val context: Context, val filename: String) {
    val prefs: SharedPreferences =
        context.getSharedPreferences(filename, Context.MODE_PRIVATE)

    fun export(): Map<String, Any?> =
        prefs.all

    fun clear() =
        prefs.edit { clear() }

    fun import(settings: Map<String, Any?>) {
        prefs.edit {
            clear()
            for ((key, value) in settings) {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    else -> {}
                }
            }
            apply()
        }
    }
}