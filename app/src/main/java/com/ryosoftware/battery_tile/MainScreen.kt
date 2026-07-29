package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ryosoftware.battery_tile.Main.Companion.findActivity
import com.ryosoftware.battery_tile.Main.Companion.hasBatteryOptimizationBypassPermission
import com.ryosoftware.battery_tile.Main.Companion.hasExactAlarmPermission
import com.ryosoftware.battery_tile.Main.Companion.hasPostNotificationsPermission
import com.ryosoftware.battery_tile.Main.Companion.requestBypassBatteryOptimizationPermission
import com.ryosoftware.battery_tile.Main.Companion.requestPostExactAlarmPermission
import com.ryosoftware.battery_tile.Main.Companion.requestPostNotificationsPermission
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import com.ryosoftware.battery_tile.WhatAppOpens.Companion.toString
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import com.ryosoftware.battery_tile.ui.components.ExpressiveSwitch
import com.ryosoftware.battery_tile.ui.components.GlassCard
import com.ryosoftware.battery_tile.ui.components.GlassGradientBackground
import com.ryosoftware.battery_tile.ui.components.SectionHeader
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelector(
    appPrefs: AppPreferences,
    notifPrefs: NotificationPreferences,
    onTileSettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onDebugLog: () -> Unit,
    onBatteryInfo: () -> Unit,
    onBatteryHistory: () -> Unit,
) {
    var tempUnit by remember { mutableStateOf(appPrefs.temperatureUnit) }
    val context = LocalContext.current
    var notificationEnabled by remember { mutableStateOf(notifPrefs.isNotificationEnabled) }
    val hasBatteryOptimizationPermission = remember { mutableStateOf(context.hasBatteryOptimizationBypassPermission()) }
    val hasExactAlarmPermission = remember { mutableStateOf(context.hasExactAlarmPermission()) }
    val hasNotificationPermission = remember { mutableStateOf(context.hasPostNotificationsPermission()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission.value = context.hasPostNotificationsPermission()
        hasBatteryOptimizationPermission.value = context.hasBatteryOptimizationBypassPermission()
        hasExactAlarmPermission.value = context.hasExactAlarmPermission()
    }

    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var importDataBytes by remember { mutableStateOf<ByteArray?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                importDataBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                showImportDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val backupData = BackupManager(context).exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(backupData) }
                    Toast.makeText(context, R.string.backup_exported, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.backup_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showImportDialog && importDataBytes != null) {
        ImportOptionsDialog(
            onDismiss = {
                showImportDialog = false
                importDataBytes = null
            },
            onConfirm = { importConfig, importData ->
                showImportDialog = false
                scope.launch {
                    try {
                        BackupManager(context).importBackup(importDataBytes!!, importConfig, importData)
                        Toast.makeText(context, R.string.backup_imported, Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                        exitProcess(0)
                    } catch (e: Exception) {
                        Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show()
                    }
                    importDataBytes = null
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GlassGradientBackground(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsCard(
                    icon = Icons.Filled.BatteryChargingFull,
                    title = stringResource(R.string.battery_information),
                    subtitle = stringResource(R.string.shows_realtime_data),
                    onClick = onBatteryInfo,
                )

                SettingsCard(
                    icon = Icons.Filled.Widgets,
                    title = stringResource(R.string.tile_settings_title),
                    subtitle = stringResource(R.string.tile_settings_body),
                    onClick = onTileSettings,
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    vibrant = true,
                ) {
                    val serviceRunning by NotificationService.isRunning.collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                notificationEnabled = !notificationEnabled
                                notifPrefs.isNotificationEnabled = notificationEnabled
                                if (notificationEnabled && !hasNotificationPermission.value) {
                                    val activity = context.findActivity()
                                    activity?.requestPostNotificationsPermission()
                                }
                                NotificationService.runOrStop(context)
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.allow_background_service_execution),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
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

                        ExpressiveSwitch(
                            checked = notificationEnabled,
                            onCheckedChange = null,
                        )
                    }
                }

                SettingsCard(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.notification_settings_title),
                    subtitle = if (notificationEnabled) stringResource(R.string.notification_settings_body)
                               else stringResource(R.string.notification_settings_body) + "\n" + stringResource(R.string.requires_background_running),
                    onClick = onNotificationSettings,
                )

                SettingsCard(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.battery_history),
                    subtitle = if (notificationEnabled) stringResource(R.string.shows_historical_data)
                               else stringResource(R.string.shows_historical_data) + "\n" + stringResource(R.string.requires_background_running),
                    onClick = onBatteryHistory,
                )

                val isExactAlarmPermissionGranted = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) || hasExactAlarmPermission.value

                if ((!hasNotificationPermission.value) || (!hasBatteryOptimizationPermission.value) || (!isExactAlarmPermissionGranted)) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = stringResource(R.string.permissions_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.permissions_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val activity = context.findActivity()
                                    activity?.requestPostNotificationsPermission()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !hasNotificationPermission.value,
                            ) {
                                Text(stringResource(R.string.request_notification_permission))
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    @SuppressLint("BatteryLife")
                                    context.requestBypassBatteryOptimizationPermission()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !hasBatteryOptimizationPermission.value,
                            ) {
                                Text(stringResource(R.string.request_battery_optimization_permission))
                            }

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { context.requestPostExactAlarmPermission() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !hasExactAlarmPermission.value,
                                ) {
                                    Text(stringResource(R.string.request_exact_alarm_permission))
                                }
                            }
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = stringResource(R.string.other_settings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.temperature_unit),
                            style = MaterialTheme.typography.titleSmall,
                        )

                        Spacer(Modifier.height(8.dp))

                        TemperatureUnit.entries.forEach { unit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempUnit = unit
                                        appPrefs.temperatureUnit = unit
                                    }
                                    .padding(vertical = 4.dp),
                            ) {
                                RadioButton(
                                    selected = tempUnit == unit,
                                    onClick = null,
                                )
                                Text(
                                    text = unit.toString(context),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.what_opens_when_user_clicks_tile_or_notification),
                            style = MaterialTheme.typography.titleSmall,
                        )

                        Spacer(Modifier.height(8.dp))

                        var whatAppOpens by remember { mutableStateOf(appPrefs.whatAppOpensWhenUserClicksTileOrNotification) }

                        WhatAppOpens.entries.forEach { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        whatAppOpens = app
                                        appPrefs.whatAppOpensWhenUserClicksTileOrNotification = app
                                    }
                                    .padding(vertical = 4.dp),
                            ) {
                                RadioButton(
                                    selected = whatAppOpens == app,
                                    onClick = null,
                                )
                                Text(
                                    text = app.toString(context),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                SettingsCard(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.debug_information),
                    subtitle = when {
                        appPrefs.isLoggingToFile && appPrefs.isLoggingOnlyWhileCharging -> stringResource(R.string.debug_information_enabled_but_only_while_charging)
                        appPrefs.isLoggingToFile -> stringResource(R.string.debug_information_enabled)
                        else -> stringResource(R.string.debug_information_disabled)
                    },
                    onClick = onDebugLog,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SectionHeader(
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.backup_restore_body),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("battery-tile-settings.json") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.export_backup))
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.import_backup))
                    }
                }

                val uriHandler = LocalUriHandler.current
                val githubRepo = stringResource(R.string.github_repo)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(githubRepo) },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        vibrant = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (importConfig: Boolean, importData: Boolean) -> Unit,
) {
    var importConfig by remember { mutableStateOf(true) }
    var importData by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { importConfig = !importConfig }
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = importConfig,
                        onCheckedChange = { importConfig = it },
                    )
                    Text(
                        text = stringResource(R.string.import_config),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { importData = !importData }
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = importData,
                        onCheckedChange = { importData = it },
                    )
                    Text(
                        text = stringResource(R.string.import_data),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(importConfig, importData) }) {
                Text(stringResource(R.string.import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
