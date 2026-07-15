package com.ryosoftware.battery_tile

import android.content.Context
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.BatteryRepository
import com.ryosoftware.battery_tile.data.ChargingSession
import com.ryosoftware.battery_tile.data.DischargeSession
import com.ryosoftware.battery_tile.data.ScreenState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

private fun Any?.toPrefEntry(): PrefEntry =
    when (this) {
        is Boolean -> PrefEntry("boolean", JsonPrimitive(this))
        is Int -> PrefEntry("int", JsonPrimitive(this))
        is Long -> PrefEntry("long", JsonPrimitive(this))
        is Float -> PrefEntry("float", JsonPrimitive(this.toDouble()))
        is String -> PrefEntry("string", JsonPrimitive(this))
        is Set<*> -> PrefEntry("string_set", JsonArray(this.map { JsonPrimitive(it.toString()) }))
        else -> PrefEntry("string", JsonPrimitive(this.toString()))
    }

private fun PrefEntry.toAny(): Any? =
    when (type) {
        "boolean" -> value.jsonPrimitive.boolean
        "int" -> value.jsonPrimitive.int
        "long" -> value.jsonPrimitive.long
        "float" -> value.jsonPrimitive.doubleOrNull?.toFloat() ?: value.jsonPrimitive.float
        "string" -> value.jsonPrimitive.content
        "string_set" -> value.jsonArray.map { it.jsonPrimitive.content }.toSet()
        else -> null
    }

@Serializable
data class PrefEntry(
    val type: String,
    val value: JsonElement
)

@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val prefs: Map<String, Map<String, PrefEntry>>,
    val batteryReadings: List<BatteryReading> = emptyList(),
    val chargingSessions: List<ChargingSession> = emptyList(),
    val dischargeSessions: List<DischargeSession> = emptyList(),
    val screenStates: List<ScreenState> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

class BackupManager(private val context: Context) {
    private val repository = BatteryRepository.getInstance(context)
    private val prefsModule = PreferencesModule(context)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportBackup(): ByteArray {
        val data = collectData()
        return json.encodeToString(data).encodeToByteArray()
    }

    suspend fun importBackup(data: ByteArray, importConfig: Boolean, importData: Boolean) {
        val backupData = json.decodeFromString<BackupData>(data.decodeToString())
        restoreConfig(backupData, importConfig, importData)
        if (importData) restoreData(backupData)
    }

    private suspend fun collectData(): BackupData {
        val prefsData = mutableMapOf<String, Map<String, PrefEntry>>()
        for (pref in prefsModule.prefs) {
            prefsData[pref.filename] = pref.export().mapValues { it.value.toPrefEntry() }
        }

        return BackupData(
            version = BackupData.CURRENT_VERSION,
            prefs = prefsData,
            batteryReadings = repository.getAllBatteryReadings().first().map { it.copy(id = 0) },
            chargingSessions = repository.getAllChargingSessions().first().map { it.copy(id = 0) },
            dischargeSessions = repository.getAllDischargeSessions().first().map { it.copy(id = 0) },
            screenStates = repository.getAllScreenStates().first().map { it.copy(id = 0) }
        )
    }

    private suspend fun restoreConfig(data: BackupData, importConfig: Boolean, importData: Boolean) {
        for (pref in prefsModule.prefs) {
            val willBeImported = (
                (importConfig && (pref !is NotificationServicePreferences)) ||
                (importData && (pref is NotificationServicePreferences))
            )
            if (willBeImported) {
                val batteryCapacityDesign = if (pref is AppPreferences) pref.batteryCapacityDesign else null
                val settings = data.prefs[pref.filename]
                if (settings != null) {
                    pref.import(settings.mapValues { it.value.toAny() })
                }
                if ((pref is AppPreferences) && (batteryCapacityDesign != null)) pref.batteryCapacityDesign = batteryCapacityDesign
            }
        }
    }

    private suspend fun restoreData(data: BackupData) {
        repository.deleteAll()
        for (reading in data.batteryReadings) {
            repository.insertBatteryReading(reading)
        }
        for (session in data.chargingSessions) {
            repository.insertChargingSession(session)
        }
        for (session in data.dischargeSessions) {
            repository.insertDischargeSession(session)
        }
        for (state in data.screenStates) {
            repository.insertScreenState(state)
        }
    }
}

