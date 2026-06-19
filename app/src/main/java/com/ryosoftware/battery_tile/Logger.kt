package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

const val TIME_REFERENCE = "TIME"

interface Logger {
    val logFileTime: StateFlow<Long>

    fun log(text: String, time: Long)
    fun log(text: String) = log(text, 0L)
    fun getLogFile(): File
    suspend fun getLogFileContents(language: String? = null): List<String>?
}

class FileLogger(context: Context) : Logger {
    private val appPrefs = AppPreferences(context)
    private val instance = context

    private val _logFileTime = MutableStateFlow(0L)

    override val logFileTime: StateFlow<Long> = _logFileTime.asStateFlow()

    companion object {
        private const val LOG_FILE = "debug_log.txt"
    }

    @Serializable
    data class LogEntry(
        val timestamp: Long,
        val time: Long,
        val message: String
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun log(text: String, time: Long) {
        if (BuildConfig.DEBUG) {
            Log.d(BuildConfig.TAG, text)
        }

        if (appPrefs.isLoggingToFile) {
            val writeToLog = !appPrefs.isLoggingOnlyWhileCharging || run {
                val batteryIntent = Main.from(instance).batteryIntentProvider.get() ?: return@run true

                val status = batteryIntent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )

                (status == BatteryManager.BATTERY_STATUS_CHARGING) || (status == BatteryManager.BATTERY_STATUS_FULL)
            }

            if (writeToLog) {
                writeLogToFile(text, time)
            }
        }
    }

    override fun log(text: String) = log(text, 0L)

    private fun writeLogToFile(message: String, time: Long) {
        try {
            val file = getLogFile()
            val logEntry = LogEntry(timestamp = System.currentTimeMillis(), time = time, message = message)
            val line = json.encodeToString(LogEntry.serializer(), logEntry)

            file.appendText("$line\n")

            _logFileTime.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(BuildConfig.TAG, e.toString())
        }
    }

    override fun getLogFile(): File = File(instance.filesDir, LOG_FILE)

    override suspend fun getLogFileContents(language: String?): List<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val locale = language?.let { Locale.forLanguageTag(it) }
                val dateFormat = locale?.let {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, it)
                } ?: DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                val timeFormat = locale?.let {
                    DateFormat.getTimeInstance(DateFormat.MEDIUM, it)
                } ?: DateFormat.getTimeInstance(DateFormat.MEDIUM)

                getLogFile().useLines { lines ->
                    lines
                        .filter { it.isNotBlank() }
                        .mapNotNull { line ->
                            runCatching {
                                json.decodeFromString<LogEntry>(line)
                            }.getOrNull()
                        }
                        .map { entry ->
                            val date = dateFormat.format(Date(entry.timestamp))
                            val text = entry.message.replace(TIME_REFERENCE, timeFormat.format(entry.time))

                            "$date $text"
                        }
                        .toList()
                }
            } catch (e: Exception) {
                Log.e(BuildConfig.TAG, e.toString())
                null
            }
        }
    }
}
