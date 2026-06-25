package com.ryosoftware.battery_tile

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class BatteryTilePreferences(context: Context) {
    companion object {
        private const val FILENAME = "battery_tile_prefs"
        const val KEY_ICON_FIELD = "icon"
        const val KEY_FIELD_POSITION_PREFIX = "tile-field-position-"
        const val KEY_FIELD_VISIBLE_PREFIX = "tile-field-visible-"
        const val KEY_FIELD_LINE_PREFIX = "tile-field-line-"
    }

    private val resources = context.resources

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

    private fun getKey(prefix: String, field: BatteryTileUIBuilder.BatteryTileField) = "$prefix${field.key.lowercase()}"

    private fun getDefaultStringFromBatteryTileField(batteryTileField: BatteryTileUIBuilder.BatteryTileField, index: Int): String? =
        if (batteryTileField.defaultsRes == 0) null else resources.getStringArray(batteryTileField.defaultsRes).getOrNull(index)

    private fun getIconFieldDefault(): BatteryTileUIBuilder.BatteryTileField {
        val value = resources.getString(R.string.tile_icon_default)
        val batteryTileField = BatteryTileUIBuilder.BatteryTileField.fromKey(value)

        return if ((batteryTileField != null) && batteryTileField.iconizable) {
            batteryTileField
        } else {
            BatteryTileUIBuilder.BatteryTileField.BATTERY_LEVEL_ICON
        }
    }

    var iconField: BatteryTileUIBuilder.BatteryTileField
        get() = prefs.getString(KEY_ICON_FIELD, null)
            ?.let(BatteryTileUIBuilder.BatteryTileField::fromKey)
            ?: getIconFieldDefault()
        set(field) { prefs.edit { putString(KEY_ICON_FIELD, field.key) } }

    private fun getFieldPositionDefault(field: BatteryTileUIBuilder.BatteryTileField): Int {
        val defaultValue = getDefaultStringFromBatteryTileField(field, 0)

        return defaultValue?.toInt() ?: Int.MAX_VALUE
    }

    fun getFieldPosition(field: BatteryTileUIBuilder.BatteryTileField): Int =
        prefs.getInt(getKey(KEY_FIELD_POSITION_PREFIX, field), getFieldPositionDefault(field))

    fun setFieldPosition(field: BatteryTileUIBuilder.BatteryTileField, position: Int) =
        prefs.edit { putInt(getKey(KEY_FIELD_POSITION_PREFIX, field), position) }

    private fun isFieldVisibleDefault(field: BatteryTileUIBuilder.BatteryTileField): Boolean {
        val defaultValue = getDefaultStringFromBatteryTileField(field, 2)

        return defaultValue?.toBoolean() ?: false
    }

    fun isFieldVisible(field: BatteryTileUIBuilder.BatteryTileField): Boolean =
        prefs.getBoolean(getKey(KEY_FIELD_VISIBLE_PREFIX, field), isFieldVisibleDefault(field))

    fun setFieldVisible(field: BatteryTileUIBuilder.BatteryTileField, visible: Boolean) =
        prefs.edit { putBoolean(getKey(KEY_FIELD_VISIBLE_PREFIX, field), visible) }

    private fun getFieldLineDefault(field: BatteryTileUIBuilder.BatteryTileField): Int {
        val defaultValue = getDefaultStringFromBatteryTileField(field, 1)

        return defaultValue?.toInt() ?: 0
    }

    fun getFieldLine(field: BatteryTileUIBuilder.BatteryTileField): Int =
        prefs.getInt(getKey(KEY_FIELD_LINE_PREFIX, field), getFieldLineDefault(field))

    fun setFieldLine(field: BatteryTileUIBuilder.BatteryTileField, line: Int) =
        prefs.edit { putInt(getKey(KEY_FIELD_LINE_PREFIX, field), line) }
}
