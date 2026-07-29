package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercent
import com.ryosoftware.battery_tile.ui.components.BatteryProgressCircle
import com.ryosoftware.battery_tile.ui.components.GlassCard
import com.ryosoftware.battery_tile.ui.components.GlassGradientBackground
import com.ryosoftware.battery_tile.ui.components.MetricRow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

const val BATTERY_CAPACITY = "battery-capacity"
const val BATTERY_PROPERTY_ENERGY_COUNTER = "battery-energy-counter"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryInfoScreen(
    prefs: BatteryTilePreferences,
    appPrefs: AppPreferences,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.battery_information),
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

            BatteryInfoContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                prefs = prefs,
                appPrefs = appPrefs,
            )
        }
    }
}

@Composable
fun BatteryInfoContent(
    modifier: Modifier = Modifier,
    prefs: BatteryTilePreferences,
    appPrefs: AppPreferences,
) {
    val context = LocalContext.current
    val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
    var batteryIntentHelper by remember { mutableStateOf(getBatteryHelper(context, batteryManager)) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryIntentHelper = getBatteryHelper(context, batteryManager)
            delay(5000.milliseconds)
        }
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val currentBatteryIntentHelper = batteryIntentHelper

            Spacer(Modifier.height(16.dp))

            if (currentBatteryIntentHelper == null) {
                Text(
                    text = stringResource(R.string.no_data_found),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val batteryLevel = currentBatteryIntentHelper.level

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    vibrant = true,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BatteryProgressCircle(
                            percentage = batteryLevel,
                            isCharging = currentBatteryIntentHelper.isCharging,
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = currentBatteryIntentHelper.toString(context, BatteryIntentHelper.BATTERY_STATUS, appPrefs, small = false) ?: stringResource(R.string.unknown_value),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val fields = listOf(
                    BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION,
                    BatteryIntentHelper.BATTERY_CHARGE,
                    BatteryIntentHelper.BATTERY_TEMPERATURE,
                    BatteryIntentHelper.BATTERY_VOLTAGE,
                    BatteryIntentHelper.BATTERY_TECHNOLOGY,
                    BatteryIntentHelper.BATTERY_HEALTH,
                    BatteryIntentHelper.BATTERY_CYCLES_COUNT,
                    BATTERY_CAPACITY,
                    BATTERY_PROPERTY_ENERGY_COUNTER,
                )
                val fieldIcons = mapOf(
                    BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION to Icons.Filled.FlashOn,
                    BatteryIntentHelper.BATTERY_CHARGE to Icons.Filled.BatteryChargingFull,
                    BatteryIntentHelper.BATTERY_TEMPERATURE to Icons.Filled.Thermostat,
                    BatteryIntentHelper.BATTERY_VOLTAGE to Icons.Filled.Bolt,
                    BatteryIntentHelper.BATTERY_TECHNOLOGY to Icons.Filled.Memory,
                    BatteryIntentHelper.BATTERY_HEALTH to Icons.Filled.HealthAndSafety,
                    BatteryIntentHelper.BATTERY_CYCLES_COUNT to Icons.Filled.Refresh,
                    BATTERY_CAPACITY to Icons.Filled.EnergySavingsLeaf,
                    BATTERY_PROPERTY_ENERGY_COUNTER to Icons.Filled.MonitorHeart,
                )
                val tempCelsius = currentBatteryIntentHelper.temperatureCelsius
                val tempAccent = when {
                    tempCelsius > 45f -> Color(0xFFE53935)
                    tempCelsius > 35f -> Color(0xFFFFA000)
                    else -> MaterialTheme.colorScheme.primary
                }

                fields.forEach { field ->
                    val label = getBatteryFieldLabel(context, field, appPrefs)
                    val value = getBatteryFieldValue(context, currentBatteryIntentHelper, field, appPrefs)

                    if ((label != null) && (value != null)) {
                        val label = label
                        val icon = fieldIcons[field] ?: Icons.Filled.BatteryFull
                        val accent = if (field == BatteryIntentHelper.BATTERY_TEMPERATURE) tempAccent
                                     else MaterialTheme.colorScheme.primary

                        MetricRow(
                            icon = icon,
                            label = label,
                            value = value,
                            accentColor = accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun getBatteryHelper(context: Context, batteryManager: BatteryManager): BatteryIntentHelper? =
    Main.from(context).batteryIntentProvider.get()?.let { BatteryIntentHelper(context, it, batteryManager) }

private fun getBatteryFieldLabel(context: Context, field: String, appPrefs: AppPreferences): String? =
    when (field) {
        BATTERY_CAPACITY -> {
            val designCapacity = appPrefs.batteryCapacityDesign
            val currentCapacity = appPrefs.batteryCapacityCurrent

            when {
                designCapacity <= 0 -> null
                currentCapacity <= 0 -> context.getString(R.string.battery_capacity_specs)
                else -> context.getString(R.string.battery_capacity_specs_vs_calculated)
            }
        }
        BATTERY_PROPERTY_ENERGY_COUNTER -> context.getString(R.string.remaining_energy)
        else -> BatteryIntentHelper.getLabel(context, field)
    }

private fun getBatteryFieldValue(context: Context, batteryIntentHelper: BatteryIntentHelper, field: String, appPrefs: AppPreferences): String? =
    when (field) {
        BATTERY_CAPACITY -> {
            val designCapacity = appPrefs.batteryCapacityDesign
            if (designCapacity <= 0) {
                null
            }
            else {
                val designCapacityString = context.getString(R.string.mah_value, designCapacity)

                val currentCapacity = appPrefs.batteryCapacityCurrent
                val currentCapacityString = if (currentCapacity > 0) { context.getString(R.string.mah_value_with_percent, currentCapacity, getStringPercent(context, (currentCapacity * 100f) / designCapacity)) } else { null }

                if (currentCapacityString == null)
                {
                    designCapacityString
                } else {
                    designCapacityString + "\n" + currentCapacityString
                }
            }
        }
        BATTERY_PROPERTY_ENERGY_COUNTER -> {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            val voltage = batteryIntentHelper.voltage
            return if ((energy != Long.MIN_VALUE) && (voltage > 0)) {
                context.getString(R.string.mah_value, ((energy / 1000f) / voltage).toInt())
            } else null
        }
        else -> return batteryIntentHelper.toString(context, field, appPrefs, false)
    }
