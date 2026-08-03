package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "screen_states")
@Serializable
data class ScreenState @Ignore constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val screenOn: Boolean,
    @Ignore val timestampString: String? = null
) {
    constructor(
        id: Long,
        timestamp: Long,
        screenOn: Boolean
    ) : this(id, timestamp, screenOn, null)
}
