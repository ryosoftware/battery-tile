package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.NotificationServiceUIBuilder.NotificationField.Companion.getComments
import com.ryosoftware.battery_tile.NotificationServiceUIBuilder.NotificationField.Companion.getLabel
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import com.ryosoftware.battery_tile.ui.components.ExpressiveSwitch
import com.ryosoftware.battery_tile.ui.components.GlassCard
import com.ryosoftware.battery_tile.ui.components.GlassGradientBackground
import com.ryosoftware.battery_tile.ui.components.GlassSurface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.ryosoftware.battery_tile.ui.components.SectionHeader
import kotlin.math.roundToInt
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

data class PrintableLastResetStatsData(
    val time: Long,
    val reason: String?,
    val batteryLevel: Int,
    val deepSleepTime: Long,
    val timeSinceBoot: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    prefs: BatteryTilePreferences,
    notifPrefs: NotificationPreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var chargedPercent by remember { mutableIntStateOf(notifPrefs.notificationChargedPercent) }
    var chargedInterval by remember { mutableIntStateOf(notifPrefs.notificationChargedInterval) }
    var chargedRepeatInterval by remember { mutableIntStateOf(notifPrefs.notificationChargedRepeatInterval) }
    var lowChargePercent by remember { mutableIntStateOf(notifPrefs.notificationLowChargePercent) }
    var lowChargeRepeatInterval by remember { mutableIntStateOf(notifPrefs.notificationLowChargeRepeatInterval) }
    var resetThreshold by remember { mutableIntStateOf(notifPrefs.resetStatsBatteryThresholdPercent) }
    var resetChargeTime by remember { mutableIntStateOf(notifPrefs.resetStatsBatteryChargeTime) }
    var autoResetEnabled by remember { mutableStateOf(notifPrefs.isAutoResetStatsEnabled) }
    var disallowDischargingNotification by remember { mutableStateOf(notifPrefs.isBlockingPowerDisconnectNotificationWhenBatteryIsCharged) }
    var showResetDialog by remember { mutableStateOf(false) }

    var expandedCharged by remember { mutableStateOf(true) }
    var expandedLow by remember { mutableStateOf(true) }
    var expandedPower by remember { mutableStateOf(true) }
    var expandedTemp by remember { mutableStateOf(true) }
    var expandedReset by remember { mutableStateOf(true) }
    var expandedFields by remember { mutableStateOf(true) }

    val appPrefs = remember { AppPreferences(context) }
    val tempUnit = appPrefs.temperatureUnit
    var tempThreshold by remember { mutableFloatStateOf(notifPrefs.getBatteryTemperatureThreshold(tempUnit)) }

    val fields = remember {
        NotificationServiceUIBuilder.NotificationField.entries.sortedWith(compareBy<NotificationServiceUIBuilder.NotificationField> { !notifPrefs.isFieldVisible(it) }.thenBy { notifPrefs.getFieldPosition(it) }).toMutableStateList()
    }

    val fieldVisibility = remember {
        mutableStateMapOf<NotificationServiceUIBuilder.NotificationField, Boolean>().apply {
            NotificationServiceUIBuilder.NotificationField.entries.forEach { put(it, notifPrefs.isFieldVisible(it)) }
        }
    }

    fun updateOrder() {
        fields.sortWith(compareBy<NotificationServiceUIBuilder.NotificationField> { !notifPrefs.isFieldVisible(it) }.thenBy { notifPrefs.getFieldPosition(it) })
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun readLastResetInfo(): PrintableLastResetStatsData {
        val persistentData = NotificationServicePreferences(context).prefs
        val time = persistentData.getLong(NotificationServicePreferences.KEY_LAST_STATS_RESET_TIME, 0L)
        val reason = persistentData.getString(NotificationServicePreferences.KEY_LAST_STATS_RESET_REASON, null)
        val batteryLevel = persistentData.getInt(NotificationServicePreferences.KEY_LAST_STATS_RESET_BATTERY_LEVEL, -1)
        val deepSleepTime = persistentData.getLong(NotificationServicePreferences.KEY_DEEP_SLEEP_TIME_AT_LAST_STATS_RESET, 0L)
        val timeSinceBoot = persistentData.getLong(NotificationServicePreferences.KEY_TIME_SINCE_BOOT_AT_LAST_STATS_RESET, 0L)
        return PrintableLastResetStatsData(time, reason, batteryLevel, deepSleepTime, timeSinceBoot)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.notification_settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GlassGradientBackground(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                ),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                item {
                    Spacer(Modifier.height(16.dp))

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

                    ExpandableSection(
                        title = stringResource(R.string.battery_charged_section_title),
                        subtitle = stringResource(R.string.battery_charged_section_body),
                        expanded = expandedCharged,
                        onToggle = { expandedCharged = !expandedCharged },
                    ) {
                        Spacer(Modifier.height(12.dp))

                        SliderRow(
                            label = stringResource(R.string.battery_charged_threshold),
                            value = chargedPercent.toFloat(),
                            onValueChange = { chargedPercent = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.notificationChargedPercent = chargedPercent },
                            valueRange = 70f..100f,
                            steps = 29,
                            displayValue = stringResource(R.string.percent_value_integer, chargedPercent),
                        )

                        Spacer(Modifier.height(8.dp))

                        SliderRow(
                            label = stringResource(R.string.battery_charged_interval),
                            value = chargedInterval.toFloat(),
                            onValueChange = { chargedInterval = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.notificationChargedInterval = chargedInterval },
                            valueRange = 0f..10f,
                            steps = 9,
                            displayValue = if (chargedInterval == 0) stringResource(R.string.no_delay) else stringResource(R.string.minutes, chargedInterval),
                        )

                        Spacer(Modifier.height(8.dp))

                        SliderRow(
                            label = stringResource(R.string.battery_charged_repeat_interval),
                            value = chargedRepeatInterval.toFloat(),
                            onValueChange = { chargedRepeatInterval = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.notificationChargedRepeatInterval = chargedRepeatInterval },
                            valueRange = 0f..10f,
                            steps = 9,
                            displayValue = if (chargedRepeatInterval == 0) stringResource(R.string.off) else stringResource(R.string.minutes, chargedRepeatInterval),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ExpandableSection(
                        title = stringResource(R.string.battery_low_section_title),
                        subtitle = stringResource(R.string.battery_low_section_body),
                        expanded = expandedLow,
                        onToggle = { expandedLow = !expandedLow },
                    ) {
                        Spacer(Modifier.height(12.dp))

                        SliderRow(
                            label = stringResource(R.string.battery_low_threshold),
                            value = lowChargePercent.toFloat(),
                            onValueChange = { lowChargePercent = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.notificationLowChargePercent = lowChargePercent },
                            valueRange = 5f..35f,
                            steps = 29,
                            displayValue = stringResource(R.string.percent_value_integer, lowChargePercent),
                        )

                        Spacer(Modifier.height(8.dp))

                        SliderRow(
                            label = stringResource(R.string.battery_low_repeat_interval),
                            value = lowChargeRepeatInterval.toFloat(),
                            onValueChange = { lowChargeRepeatInterval = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.notificationLowChargeRepeatInterval = lowChargeRepeatInterval },
                            valueRange = 0f..10f,
                            steps = 9,
                            displayValue = if (lowChargeRepeatInterval == 0) stringResource(R.string.off) else stringResource(R.string.minutes, lowChargeRepeatInterval),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ExpandableSection(
                        title = stringResource(R.string.power_connected_or_disconnected_section_title),
                        subtitle = stringResource(R.string.power_connected_or_disconnected_section_body),
                        expanded = expandedPower,
                        onToggle = { expandedPower = !expandedPower },
                    ) {
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    disallowDischargingNotification = !disallowDischargingNotification
                                    notifPrefs.isBlockingPowerDisconnectNotificationWhenBatteryIsCharged = disallowDischargingNotification
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.block_power_disconnect_notification_when_battery_is_charged),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ExpressiveSwitch(
                                checked = disallowDischargingNotification,
                                onCheckedChange = null,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    ExpandableSection(
                        title = stringResource(R.string.battery_temperature_section_title),
                        subtitle = stringResource(R.string.battery_temperature_section_body),
                        expanded = expandedTemp,
                        onToggle = { expandedTemp = !expandedTemp },
                    ) {
                        Spacer(Modifier.height(12.dp))

                        val tempThresholdMin = tempUnit.fromCelsius(28f)
                        val tempThresholdMax = tempUnit.fromCelsius(50f)
                        val tempThresholdSteps = (tempThresholdMax - tempThresholdMin).toInt() - 1

                        tempThreshold = tempThreshold.coerceIn(tempThresholdMin, tempThresholdMax)

                        SliderRow(
                            label = stringResource(R.string.battery_temperature_threshold),
                            value = tempThreshold,
                            onValueChange = { tempThreshold = it },
                            onValueChangeFinished = { notifPrefs.setBatteryTemperatureThreshold(tempThreshold, tempUnit) },
                            valueRange = tempThresholdMin..tempThresholdMax,
                            steps = tempThresholdSteps,
                            displayValue = tempUnit.toString(context, tempThreshold),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ExpandableSection(
                        title = stringResource(R.string.stats_reset_threshold_section_title),
                        subtitle = stringResource(R.string.stats_reset_threshold_section_body),
                        expanded = expandedReset,
                        enabled = autoResetEnabled,
                        onToggle = { expandedReset = !expandedReset },
                    ) {
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoResetEnabled = !autoResetEnabled
                                    notifPrefs.isAutoResetStatsEnabled = autoResetEnabled
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.auto_reset_stats),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ExpressiveSwitch(
                                checked = autoResetEnabled,
                                onCheckedChange = null,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        SliderRow(
                            label = stringResource(R.string.minimum_load_required),
                            value = resetThreshold.toFloat(),
                            onValueChange = { resetThreshold = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.resetStatsBatteryThresholdPercent = resetThreshold },
                            valueRange = 0f..10f,
                            steps = 9,
                            displayValue = stringResource(R.string.percent_value_integer, resetThreshold),
                            enabled = autoResetEnabled,
                        )

                        Spacer(Modifier.height(8.dp))

                        SliderRow(
                            label = stringResource(R.string.minimum_time_required),
                            value = resetChargeTime.toFloat(),
                            onValueChange = { resetChargeTime = it.roundToInt() },
                            onValueChangeFinished = { notifPrefs.resetStatsBatteryChargeTime = resetChargeTime },
                            valueRange = 0f..30f,
                            steps = 29,
                            displayValue = stringResource(R.string.minutes, resetChargeTime),
                            enabled = autoResetEnabled,
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
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
                                },
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        val lastStatsResetTimeString = getStringDateTime(context, lastResetInfo.time)
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

                        Text(
                            text = lastStatsResetString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    SectionHeader(
                        title = stringResource(R.string.notification_fields_section_title),
                        subtitle = stringResource(R.string.notification_fields_section_body, stringResource(R.string.since_boot), stringResource(R.string.since_last_stats_reset), ""),
                    )

                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(fields) { index, field ->
                    if (field.isSupported) {
                        val batteryLevelIsLastVisible = field == NotificationServiceUIBuilder.NotificationField.BATTERY_LEVEL && fields.all { it == field || !(fieldVisibility[it] ?: false) }
                        val comments = field.getComments(context)

                        FieldRow(
                            label = field.getLabel(context, false),
                            comments = comments,
                            checked = fieldVisibility[field] ?: false,
                            batteryLevelIsLastVisible = batteryLevelIsLastVisible,
                            onCheckedChange = {
                                val newVisible = !(fieldVisibility[field] ?: false)
                                if (!newVisible && field != NotificationServiceUIBuilder.NotificationField.BATTERY_LEVEL) {
                                    val allOthersHidden = fields.all {
                                        it == field || !(fieldVisibility[it] ?: false)
                                    }
                                    if (allOthersHidden) {
                                        fieldVisibility[NotificationServiceUIBuilder.NotificationField.BATTERY_LEVEL] = true
                                        notifPrefs.setFieldVisible(NotificationServiceUIBuilder.NotificationField.BATTERY_LEVEL, true)
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
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldRow(
    label: String,
    comments: String?,
    checked: Boolean,
    batteryLevelIsLastVisible: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (checked) 1f else 0.85f),
        vibrant = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(enabled = !batteryLevelIsLastVisible) { onCheckedChange(!checked) },
            verticalAlignment = if (comments == null) Alignment.CenterVertically else Alignment.Top,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = !batteryLevelIsLastVisible,
            )

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                if (comments != null) {
                    Text(
                        text = comments,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}


@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        vibrant = true,
        onClick = onToggle,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.then(if (!enabled) Modifier.alpha(0.4f) else Modifier),
                    )
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.then(if (!enabled) Modifier.alpha(0.4f) else Modifier),
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier.then(if (!enabled) Modifier.alpha(0.4f) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}
