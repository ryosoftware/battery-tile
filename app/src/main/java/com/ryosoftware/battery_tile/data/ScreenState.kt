package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "screen_states")
@Serializable
data class ScreenState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val screenOn: Boolean
)
