package com.ryosoftware.battery_tile

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

const val BATTERY_CAPACITY_DESIGN = "battery-capacity-design"
const val BATTERY_PROPERTY_CHARGE_COUNTER = "battery-charge-counter"
const val BATTERY_PROPERTY_ENERGY_COUNTER = "battery-energy-counter"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryInfoScreen(
    prefs: BatteryTilePreferences,
    appPrefs: AppPreferences,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_information)) },
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
        BatteryInfoContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            prefs = prefs,
            appPrefs = appPrefs
        )
    }
}

@Composable
fun BatteryInfoContent(
    modifier: Modifier = Modifier,
    prefs: BatteryTilePreferences,
    appPrefs: AppPreferences,
) {
    val context = LocalContext.current
    var batteryIntentHelper by remember { mutableStateOf(getBatteryHelper(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryIntentHelper = getBatteryHelper(context)
            delay(5000.milliseconds)
        }
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val currentBatteryIntentHelper = batteryIntentHelper

            Spacer(Modifier.height(16.dp))

            if (currentBatteryIntentHelper == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.no_data_found),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                val fields = listOf(
                    BatteryIntentHelper.BATTERY_LEVEL,
                    BatteryIntentHelper.BATTERY_STATUS,
                    BatteryIntentHelper.BATTERY_CAPACITY,
                    BatteryIntentHelper.BATTERY_TEMPERATURE,
                    BatteryIntentHelper.BATTERY_VOLTAGE,
                    BatteryIntentHelper.BATTERY_TECHNOLOGY,
                    BatteryIntentHelper.BATTERY_HEALTH,
                    BatteryIntentHelper.BATTERY_CYCLES_COUNT,
                    BATTERY_CAPACITY_DESIGN,
                    BATTERY_PROPERTY_CHARGE_COUNTER,
                    BATTERY_PROPERTY_ENERGY_COUNTER,
                )

                fields.forEachIndexed { index, field ->
                    val isAvailable = isBatteryFieldAvailable(context,currentBatteryIntentHelper, field, appPrefs)

                    if (isAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = getBatteryFieldLabel(context, field),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1.2f)
                            )

                            Text(
                                text = getBatteryFieldValue(context, currentBatteryIntentHelper, field, appPrefs),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End
                            )
                        }

                        if (index < fields.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (batteryIntentHelper?.capacity?.let { it >= 0 } == true) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.battery_capacity_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun getBatteryHelper(context: Context): BatteryIntentHelper? =
    Main.from(context).batteryIntentProvider.get()?.let { BatteryIntentHelper(it) }

private fun getBatteryFieldLabel(context: Context, field: String): String =
    when (field) {
        BATTERY_CAPACITY_DESIGN -> context.getString(R.string.battery_capacity_design)

        BATTERY_PROPERTY_CHARGE_COUNTER -> context.getString(R.string.remaining_charge)

        BATTERY_PROPERTY_ENERGY_COUNTER -> context.getString(R.string.remaining_energy)

        else -> BatteryIntentHelper.getLabel(context, field)
    }

private fun getBatteryFieldValue(context: Context, batteryIntentHelper: BatteryIntentHelper, field: String, appPrefs: AppPreferences): String =
    when (field) {
        BATTERY_CAPACITY_DESIGN -> {
            val capacity = appPrefs.batteryCapacityDesign
            if (capacity > 0) {
                return context.getString(R.string.mah_value, capacity)
            }
            return context.getString(R.string.not_available)
        }

        BATTERY_PROPERTY_CHARGE_COUNTER -> {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            return context.getString(R.string.mah_value, (value / 1000f).toInt())
        }

        BATTERY_PROPERTY_ENERGY_COUNTER -> {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            val voltage = batteryIntentHelper.voltage
            if ((energy != Long.MIN_VALUE) && (voltage > 0)) {
                return context.getString(R.string.mah_value, ((energy / 1000f) / voltage).toInt())
            }
            return context.getString(R.string.not_available)
        }

        else -> return batteryIntentHelper.toString(context, field, appPrefs, false)
    }


private fun isBatteryFieldAvailable(context: Context, batteryIntentHelper: BatteryIntentHelper, field: String, appPrefs: AppPreferences): Boolean =
    when (field) {
        BATTERY_CAPACITY_DESIGN -> appPrefs.batteryCapacityDesign > 0

        BATTERY_PROPERTY_CHARGE_COUNTER -> true

        BATTERY_PROPERTY_ENERGY_COUNTER -> {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

            energy != Long.MIN_VALUE
        }
        else -> batteryIntentHelper.isValid(field)
    }
