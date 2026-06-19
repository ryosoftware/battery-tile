package com.ryosoftware.battery_tile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.ryosoftware.battery_tile.AlarmsReceiver.Tags.Companion.execute
import com.ryosoftware.battery_tile.AlarmsReceiver.Tags.Companion.getPendingIntent

class AlarmsReceiver : BroadcastReceiver() {
    enum class Tags(val key: String, val requestCode: Int, val label: String) {
        NOTIFICATION_SERVICE_PERSIST_DATA("NOTIFICATION-SERVICE-PERSIST-DATA", 1001, "persist data at Notification Service"),
        NOTIFICATION_SERVICE_UPDATE_CHARGED_NOTIFICATION("NOTIFICATION-SERVICE-UPDATE-CHARGED-NOTIFICATION", 1002,"repost Charged Notification at Notification Service");

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): Tags? = map[key?.uppercase()]

            fun Tags.execute(context: Context) =
                when (this) {
                    NOTIFICATION_SERVICE_PERSIST_DATA ->
                        NotificationService.runOrStop(
                            context,
                            NotificationService.ACTION_PERSIST_DATA_ALARM
                        )
                    NOTIFICATION_SERVICE_UPDATE_CHARGED_NOTIFICATION ->
                        NotificationService.runOrStop(
                            context,
                            NotificationService.ACTION_UPDATE_CHARGED_NOTIFICATION_ALARM
                        )
                }

            fun Tags.getPendingIntent(context: Context, forStop: Boolean): PendingIntent? {
                val intent = Intent(context, AlarmsReceiver::class.java).apply{
                    action = ACTION_ALARM
                    putExtra(EXTRA_TAG, key)
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }

                val flags = if (forStop) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_IMMUTABLE

                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    flags
                )
            }
        }
    }
    companion object {
        private const val FILENAME = "alarms_receiver_prefs"

        const val KEY_ACTIVE_TAG_PREFIX = "active-tag-"

        private const val ACTION_ALARM = "${BuildConfig.APPLICATION_ID}.ALARM"
        private const val EXTRA_TAG = "tag"
        private const val EXTRA_SESSION_ID = "session-id"

        private val sessionId = System.currentTimeMillis()

        private fun getLogger(context: Context): Logger = Main.from(context).logger

        private fun getPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

        private fun getKey(prefix: String, tag: Tags): String = "$prefix${tag.key.lowercase()}"

        fun start(context: Context, tag: Tags, millis: Long) {
            stop(context, tag)

            val triggerTime = System.currentTimeMillis() + millis

            getLogger(context).log("Scheduling alarm event for ${tag.key} in ${millis / 1000} seconds, at $TIME_REFERENCE", triggerTime)

            getPreferences(context).edit {
                putBoolean(getKey(KEY_ACTIVE_TAG_PREFIX, tag), true)
            }

            val pendingIntent = tag.getPendingIntent(context, false) ?: return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.S) || alarmManager.canScheduleExactAlarms())
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            else
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
        }

        fun stop(context: Context, tag: Tags) {
            getLogger(context).log("Stopping alarm events related to ${tag.key}")

            getPreferences(context).edit {
                remove(getKey(KEY_ACTIVE_TAG_PREFIX, tag))
            }

            val pendingIntent = tag.getPendingIntent(context, true)

            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val logger = Main.from(context).logger

        logger.log("Received a ${action?.substringAfterLast(".")} event")

        if (action != ACTION_ALARM) return

        val extraTag = intent.getStringExtra(EXTRA_TAG)
        val tag = Tags.fromKey(extraTag) ?: return

        val intentSessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        if (sessionId != intentSessionId) return

        val prefs = getPreferences(context)
        if (!prefs.getBoolean(getKey(KEY_ACTIVE_TAG_PREFIX, tag), false)) return

        logger.log("Going to ${tag.label}")

        tag.execute(context)
    }
}
