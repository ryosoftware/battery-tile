package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_states")
data class ScreenState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val screenOn: Boolean
)
