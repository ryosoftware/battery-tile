package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ryosoftware.battery_tile.Main.Companion.hasBatteryOptimizationPermission
import com.ryosoftware.battery_tile.Main.Companion.hasExactAlarmPermission
import com.ryosoftware.battery_tile.Main.Companion.requestPostExactAlarmPermission
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import com.ryosoftware.battery_tile.WhatAppOpens.Companion.toString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelector(
    appPrefs: AppPreferences,
    notifPrefs: NotificationPreferences,
    onTileSettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onDebugLog: () -> Unit,
    onBatteryInfo: () -> Unit
) {
    var tempUnit by remember { mutableStateOf(appPrefs.temperatureUnit) }
    val context = LocalContext.current
    val hasBatteryOptimizationPermission = remember { mutableStateOf(context.hasBatteryOptimizationPermission()) }
    val hasExactAlarmPermission = remember { mutableStateOf(context.hasExactAlarmPermission()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = onTileSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.tile_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Card(
                    onClick = onNotificationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val serviceEnabled = notifPrefs.isNotificationEnabled
                    val serviceRunning by NotificationService.isRunning.collectAsState()

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.notification_settings),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (serviceEnabled && serviceRunning) stringResource(R.string.service_enabled_and_running)
                                   else if (serviceEnabled) stringResource(R.string.service_enabled_but_not_running)
                                   else stringResource(R.string.service_not_allowed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (serviceEnabled && serviceRunning) MaterialTheme.colorScheme.primary
                                    else if (serviceEnabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.temperature_unit),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                    )

                    TemperatureUnit.entries.forEach { unit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .clickable {
                                    tempUnit = unit
                                    appPrefs.temperatureUnit = unit
                                }
                        ) {
                            RadioButton(
                                selected = tempUnit == unit,
                                onClick = null
                            )
                            Text(
                                text = unit.toString(context),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var whatAppOpens by remember { mutableStateOf(appPrefs.whatAppOpensWhenUserClicksTileOrNotification) }

                    Text(
                        text = stringResource(R.string.what_opens_when_user_clicks_tile_or_notification),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                    )

                    WhatAppOpens.entries.forEach { app ->
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    .clickable {
                                        whatAppOpens = app
                                        appPrefs.whatAppOpensWhenUserClicksTileOrNotification = app
                                    }
                        ) {
                            RadioButton(
                                selected = whatAppOpens == app,
                                onClick = null
                            )
                            Text(
                                text = app.toString(context),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                Card(
                    onClick = onBatteryInfo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.battery_information),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Card(
                    onClick = onDebugLog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val loggingEnabled = appPrefs.isLoggingToFile
                    val loggingOnlyWhileCharging = appPrefs.isLoggingOnlyWhileCharging

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.debug_information),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (loggingEnabled && loggingOnlyWhileCharging) stringResource(R.string.debug_information_enabled_but_only_while_charging)
                                   else if (loggingEnabled) stringResource(R.string.debug_information_enabled)
                                   else stringResource(R.string.debug_information_disabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (loggingEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = {
                        @SuppressLint("BatteryLife")
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !hasBatteryOptimizationPermission.value
                ) {
                    Text(
                        text = stringResource(R.string.request_battery_optimization_permission)
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(
                        onClick = { context.requestPostExactAlarmPermission() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !hasExactAlarmPermission.value
                    ) {
                        Text(
                            text = stringResource(R.string.request_exact_alarm_permission)
                        )
                    }
                }
            }

            val uriHandler = LocalUriHandler.current
            val githubRepo = stringResource(R.string.github_repo)

            Text(
                text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { uriHandler.openUri(githubRepo) },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
