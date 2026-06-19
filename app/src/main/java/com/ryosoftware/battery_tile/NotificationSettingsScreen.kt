package com.ryosoftware.battery_tile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import java.util.Date
import kotlin.math.roundToInt
import android.content.IntentFilter
import com.ryosoftware.battery_tile.Main.Companion.hasPostNotificationsPermission
import com.ryosoftware.battery_tile.NotificationBatteryIntentHelper.NotificationField.Companion.getLabel
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString

data class PrintableLastResetStatsData(
    val time: Long,
    val reason: String?,
    val batteryLevel: Int,
    val deepSleepTime: Long,
    val timeSinceBoot: Long
)

@Composable
private fun InfoCard(
    title: String,
    body: String,
    enabled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                .then(if (!enabled) Modifier.alpha(0.4f) else Modifier)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                .then(if (!enabled) Modifier.alpha(0.4f) else Modifier)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    prefs: BatteryTilePreferences,
    notifPrefs: NotificationPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var notificationEnabled by remember { mutableStateOf(notifPrefs.isNotificationEnabled) }
    var chargedPercent by remember { mutableIntStateOf(notifPrefs.notificationChargedPercent) }
    var chargedInterval by remember { mutableIntStateOf(notifPrefs.notificationChargedInterval) }
    var repeatInterval by remember { mutableIntStateOf(notifPrefs.notificationChargedRepeatInterval) }
    var resetThreshold by remember { mutableIntStateOf(notifPrefs.resetStatsBatteryThresholdPercent) }
    var resetChargeTime by remember { mutableIntStateOf(notifPrefs.resetStatsBatteryChargeTime) }
    var autoResetEnabled by remember { mutableStateOf(notifPrefs.isAutoResetStatsEnabled) }
    var disallowDischargingNotification by remember { mutableStateOf(notifPrefs.isBlockingPowerDisconnectNotificationWhenBatteryIsCharged) }
    var showResetDialog by remember { mutableStateOf(false) }

    val appPrefs = remember { AppPreferences(context) }
    val tempUnit = appPrefs.temperatureUnit
    var tempThreshold by remember { mutableFloatStateOf(notifPrefs.getBatteryTemperatureThreshold(tempUnit)) }

    val fields = remember {
        NotificationBatteryIntentHelper.NotificationField.entries.sortedWith(compareBy<NotificationBatteryIntentHelper.NotificationField> { !notifPrefs.isFieldVisible(it) }.thenBy { notifPrefs.getFieldPosition(it) }).toMutableStateList()
    }

    val fieldVisibility = remember {
        mutableStateMapOf<NotificationBatteryIntentHelper.NotificationField, Boolean>().apply {
            NotificationBatteryIntentHelper.NotificationField.entries.forEach { put(it, notifPrefs.isFieldVisible(it)) }
        }
    }

    fun updateOrder() {
        fields.sortWith(compareBy<NotificationBatteryIntentHelper.NotificationField> { !notifPrefs.isFieldVisible(it) }.thenBy { notifPrefs.getFieldPosition(it) })
    }

    val hasNotificationPermission = remember { mutableStateOf(context.hasPostNotificationsPermission()) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted -> hasNotificationPermission.value = granted }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun readLastResetInfo(): PrintableLastResetStatsData {
        val persistentData = NotificationService.getPersistentDataPreferences(context)
        val time = persistentData.getLong(NotificationService.KEY_LAST_STATS_RESET_TIME, 0L)
        val reason = persistentData.getString(NotificationService.KEY_LAST_STATS_RESET_REASON, null)
        val batteryLevel = persistentData.getInt(NotificationService.KEY_LAST_STATS_RESET_BATTERY_LEVEL, -1)
        val deepSleepTime = persistentData.getLong(NotificationService.KEY_DEEP_SLEEP_TIME_AT_LAST_STATS_RESET, 0L)
        val timeSinceBoot = persistentData.getLong(NotificationService.KEY_TIME_SINCE_BOOT_AT_LAST_STATS_RESET, 0L)
        return PrintableLastResetStatsData(time, reason, batteryLevel, deepSleepTime, timeSinceBoot)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))

                val serviceRunning by NotificationService.isRunning.collectAsState()

                var lastResetInfo by remember { mutableStateOf(readLastResetInfo()) }

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            lastResetInfo = readLastResetInfo()
                        }
                    }
                    val filter = IntentFilter(NotificationService.ACTION_RESET_STATS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        @SuppressLint("UnspecifiedRegisterReceiverFlag")
                        context.registerReceiver(receiver, filter)
                    }
                    onDispose { context.unregisterReceiver(receiver) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            notificationEnabled = !notificationEnabled
                            notifPrefs.isNotificationEnabled = notificationEnabled
                            if (notificationEnabled && !hasNotificationPermission.value) {
                                @SuppressLint("InlinedApi")
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            NotificationService.runOrStop(context)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.allow_background_service_execution),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (notificationEnabled && serviceRunning) stringResource(R.string.service_enabled_and_running)
                                   else if (notificationEnabled) stringResource(R.string.service_enabled_but_not_running)
                                   else stringResource(R.string.service_not_allowed),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (notificationEnabled && serviceRunning) MaterialTheme.colorScheme.primary
                                    else if (notificationEnabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = null
                    )
                }

                Button(
                    onClick = {
                        @SuppressLint("InlinedApi")
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = notificationEnabled && !hasNotificationPermission.value
                ) {
                    Text(
                        text = stringResource(R.string.request_notification_permission)
                    )
                }

                Spacer(Modifier.height(12.dp))

                InfoCard(
                    title = stringResource(R.string.battery_charged_section_title),
                    body = stringResource(R.string.battery_charged_section_body),
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.battery_charged_threshold),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.percent_value_integer, chargedPercent),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val chargePercentMin = 70f
                val chargePercentMax = 100f
                val chargePercentSteps = (chargePercentMax - chargePercentMin).toInt() - 1

                chargedPercent = chargedPercent.coerceIn(chargePercentMin.toInt(), chargePercentMax.toInt())

                Slider(
                    value = chargedPercent.toFloat(),
                    onValueChange = { chargedPercent = it.roundToInt() },
                    onValueChangeFinished = {
                        notifPrefs.notificationChargedPercent = chargedPercent
                    },
                    valueRange = chargePercentMin..chargePercentMax,
                    steps = chargePercentSteps,
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.battery_charged_interval),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (chargedInterval == 0) stringResource(R.string.no_delay) else stringResource(R.string.minutes, chargedInterval),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val chargedIntervalMin = 0f
                val chargedIntervalMax = 10f
                val chargedIntervalSteps = (chargedIntervalMax - chargedIntervalMin).toInt() - 1

                chargedInterval = chargedInterval.coerceIn(chargedIntervalMin.toInt(), chargedIntervalMax.toInt())

                Slider(
                    value = chargedInterval.toFloat(),
                    onValueChange = { chargedInterval = it.roundToInt() },
                    onValueChangeFinished = {
                        notifPrefs.notificationChargedInterval = chargedInterval
                    },
                    valueRange = chargedIntervalMin..chargedIntervalMax,
                    steps = chargedIntervalSteps,
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.battery_charged_repeat_interval),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (repeatInterval == 0) stringResource(R.string.off) else stringResource(R.string.minutes, repeatInterval),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val repeatIntervalMin = 0f
                val repeatIntervalMax = 10f
                val repeatIntervalSteps = (repeatIntervalMax - repeatIntervalMin).toInt() - 1

                repeatInterval = repeatInterval.coerceIn(repeatIntervalMin.toInt(), repeatIntervalMax.toInt())

                Slider(
                    value = repeatInterval.toFloat(),
                    onValueChange = { repeatInterval = it.roundToInt() },
                    onValueChangeFinished = {
                        notifPrefs.notificationChargedRepeatInterval = repeatInterval
                    },
                    valueRange = repeatIntervalMin..repeatIntervalMax,
                    steps = repeatIntervalSteps,
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))

                InfoCard(
                    title = stringResource(R.string.power_connected_or_disconnected_section_title),
                    body = stringResource(R.string.power_connected_or_disconnected_section_body),
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier)
                        .clickable(enabled = notificationEnabled) {
                            disallowDischargingNotification = !disallowDischargingNotification
                            notifPrefs.isBlockingPowerDisconnectNotificationWhenBatteryIsCharged = disallowDischargingNotification
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.block_power_disconnect_notification_when_battery_is_charged),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = disallowDischargingNotification,
                        onCheckedChange = null,
                        enabled = notificationEnabled
                    )
                }

                Spacer(Modifier.height(12.dp))

                InfoCard(
                    title = stringResource(R.string.battery_temperature_section_title),
                    body = stringResource(R.string.battery_temperature_section_body),
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.battery_temperature_threshold),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = tempUnit.toString(context, tempThreshold),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val tempThresholdMin = tempUnit.fromCelsius(28f)
                val tempThresholdMax = tempUnit.fromCelsius(50f)
                val tempThresholdSteps = (tempThresholdMax - tempThresholdMin).toInt() - 1

                tempThreshold = tempThreshold.coerceIn(tempThresholdMin, tempThresholdMax)

                Slider(
                    value = tempThreshold,
                    onValueChange = { tempThreshold = it },
                    onValueChangeFinished = {
                        notifPrefs.setBatteryTemperatureThreshold(tempThreshold, tempUnit)
                    },
                    valueRange = tempThresholdMin..tempThresholdMax,
                    steps = tempThresholdSteps,
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))

                InfoCard(
                    title = stringResource(R.string.stats_reset_threshold_section_title),
                    body = stringResource(R.string.stats_reset_threshold_section_body),
                    enabled = notificationEnabled && autoResetEnabled
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier)
                        .clickable(enabled = notificationEnabled) {
                            autoResetEnabled = !autoResetEnabled
                            notifPrefs.isAutoResetStatsEnabled = autoResetEnabled
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.auto_reset_stats),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = autoResetEnabled,
                        onCheckedChange = null,
                        enabled = notificationEnabled
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(if ((!notificationEnabled) || (!autoResetEnabled)) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.minimum_load_required),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.percent_value_integer, resetThreshold),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val resetThresholdMin = 0f
                val resetThresholdMax = 10f
                val resetThresholdSteps = (resetThresholdMax - resetThresholdMin).toInt() - 1

                resetThreshold = resetThreshold.coerceIn(resetThresholdMin.toInt(), resetThresholdMax.toInt())

                Slider(
                    value = resetThreshold.toFloat(),
                    onValueChange = { resetThreshold = it.roundToInt() },
                    onValueChangeFinished = {
                        notifPrefs.resetStatsBatteryThresholdPercent = resetThreshold
                    },
                    valueRange = resetThresholdMin..resetThresholdMax,
                    steps = resetThresholdSteps,
                    enabled = notificationEnabled && autoResetEnabled
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(if ((!notificationEnabled) || (!autoResetEnabled)) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.minimum_time_required),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.minutes, resetChargeTime),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                val resetChargeTimeMin = 0f
                val resetChargeTimeMax = 30f
                val resetChargeTimeSteps = (resetChargeTimeMax - resetChargeTimeMin).toInt() - 1

                resetChargeTime = resetChargeTime.coerceIn(resetChargeTimeMin.toInt(), resetChargeTimeMax.toInt())

                Slider(
                    value = resetChargeTime.toFloat(),
                    onValueChange = { resetChargeTime = it.roundToInt() },
                    onValueChangeFinished = {
                        notifPrefs.resetStatsBatteryChargeTime = resetChargeTime
                    },
                    valueRange = resetChargeTimeMin..resetChargeTimeMax,
                    steps = resetChargeTimeSteps,
                    enabled = notificationEnabled && autoResetEnabled
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = notificationEnabled
                ) {
                    Text(stringResource(R.string.reset_stats_now))
                }

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text(stringResource(R.string.reset_stats_confirm_title)) },
                        text = { Text(stringResource(R.string.reset_stats_confirm_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                NotificationService.resetStats(context)
                                showResetDialog = false
                            }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                val lastStatsResetTimeString = java.text.DateFormat
                    .getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.MEDIUM
                    )
                    .format(Date(lastResetInfo.time))

                val lastStatsResetReasonString = when (lastResetInfo.reason) {
                    LastStatsResetReason.POWER_DISCONNECTED.key -> stringResource(R.string.power_disconnected)
                    LastStatsResetReason.USER_REQUEST.key -> stringResource(R.string.user_request)
                    LastStatsResetReason.EXPIRED_DATA.key -> stringResource(R.string.expired_data)
                    LastStatsResetReason.DEVICE_REBOOT.key -> stringResource(R.string.device_reboot)
                    else -> stringResource(R.string.no_data_found)
                }

                val lastResetDeepSleepPercent = if (lastResetInfo.timeSinceBoot == 0L) 100 else (lastResetInfo.deepSleepTime * 100f / lastResetInfo.timeSinceBoot).toInt().coerceAtMost(100)
                val lastResetDeepSleepPercentString = stringResource(R.string.percent_value_integer, lastResetDeepSleepPercent)

                val batteryLevelString = if (lastResetInfo.batteryLevel < 0) null else stringResource(R.string.percent_value_integer, lastResetInfo.batteryLevel)

                val lastStatsResetString = if (lastResetInfo.time == 0L) ""
                                           else if (!batteryLevelString.isNullOrEmpty()) stringResource(R.string.reset_time_and_reason_and_deep_sleep_and_battery_level, lastStatsResetTimeString, lastStatsResetReasonString, lastResetDeepSleepPercentString, batteryLevelString)
                                           else stringResource(R.string.reset_time_and_reason_and_deep_sleep, lastStatsResetTimeString, lastStatsResetReasonString, lastResetDeepSleepPercentString)

                InfoCard(
                    title = stringResource(R.string.notification_fields_section_title),
                    body = stringResource(R.string.notification_fields_section_body, stringResource(R.string.since_boot), stringResource(R.string.since_last_stats_reset), lastStatsResetString),
                    enabled = notificationEnabled
                )

                Spacer(Modifier.height(12.dp))
            }

            itemsIndexed(fields) { index, field ->
                if (field.isSupported) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!notificationEnabled) Modifier.alpha(0.4f) else Modifier)
                    ) {
                        val batteryLevelIsLastVisible = field == NotificationBatteryIntentHelper.NotificationField.BATTERY_LEVEL &&
                            fields.all { it == field || !(fieldVisibility[it] ?: false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .clickable(enabled = notificationEnabled && !batteryLevelIsLastVisible) {
                                    val newVisible = !(fieldVisibility[field] ?: false)
                                    if (!newVisible && field != NotificationBatteryIntentHelper.NotificationField.BATTERY_LEVEL) {
                                        val allOthersHidden = fields.all {
                                            it == field || !(fieldVisibility[it] ?: false)
                                        }
                                        if (allOthersHidden) {
                                            fieldVisibility[NotificationBatteryIntentHelper.NotificationField.BATTERY_LEVEL] = true
                                            notifPrefs.setFieldVisible(NotificationBatteryIntentHelper.NotificationField.BATTERY_LEVEL, true)
                                        }
                                    }
                                    fieldVisibility[field] = newVisible
                                    notifPrefs.setFieldVisible(field, newVisible)
                                    if (newVisible) {
                                        fields.filter { it != field }.forEach {
                                            notifPrefs.setFieldPosition(it, notifPrefs.getFieldPosition(it) + 1)
                                        }
                                        notifPrefs.setFieldPosition(field, 1)
                                    } else {
                                        val maxPos = fields.maxOf { notifPrefs.getFieldPosition(it) }
                                        notifPrefs.setFieldPosition(field, maxPos + 1)
                                    }
                                    updateOrder()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = fieldVisibility[field] ?: false,
                                onCheckedChange = null,
                                enabled = notificationEnabled && !batteryLevelIsLastVisible
                            )

                            Text(
                                text = field.getLabel(context, false),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
