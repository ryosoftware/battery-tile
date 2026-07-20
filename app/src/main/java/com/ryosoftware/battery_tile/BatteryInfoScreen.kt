package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

const val BATTERY_CAPACITY_DESIGN = "battery-capacity-design"
const val BATTERY_CAPACITY_CURRENT = "battery-capacity-current"
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
                    BatteryIntentHelper.BATTERY_TEMPERATURE,
                    BatteryIntentHelper.BATTERY_VOLTAGE,
                    BatteryIntentHelper.BATTERY_TECHNOLOGY,
                    BatteryIntentHelper.BATTERY_HEALTH,
                    BatteryIntentHelper.BATTERY_CYCLES_COUNT,
                    BATTERY_CAPACITY_DESIGN,
                    BATTERY_CAPACITY_CURRENT,
                    BatteryIntentHelper.BATTERY_CHARGE,
                    BATTERY_PROPERTY_ENERGY_COUNTER,
                    BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION,
                )

                fields.forEachIndexed { index, field ->
                    val text = getBatteryFieldValue(context,currentBatteryIntentHelper, field, appPrefs)

                    if (text != null) {
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
                                text = text,
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
    }
}

private fun getBatteryHelper(context: Context, batteryManager: BatteryManager): BatteryIntentHelper? =
    Main.from(context).batteryIntentProvider.get()?.let { BatteryIntentHelper(it, batteryManager) }

private fun getBatteryFieldLabel(context: Context, field: String): String =
    when (field) {
        BATTERY_CAPACITY_DESIGN -> context.getString(R.string.battery_capacity_design)

        BATTERY_CAPACITY_CURRENT -> context.getString(R.string.battery_capacity_current)

        BATTERY_PROPERTY_ENERGY_COUNTER -> context.getString(R.string.remaining_energy)

        else -> BatteryIntentHelper.getLabel(context, field)
    }

private fun getBatteryFieldValue(context: Context, batteryIntentHelper: BatteryIntentHelper, field: String, appPrefs: AppPreferences): String? =
    when (field) {
        BATTERY_CAPACITY_DESIGN -> {
            val capacity = appPrefs.batteryCapacityDesign

            return if (capacity > 0) {
                context.getString(R.string.mah_value, capacity)
            } else null
        }

        BATTERY_CAPACITY_CURRENT -> {
            val designCapacity = appPrefs.batteryCapacityDesign
            val currentCapacity = appPrefs.batteryCapacityCurrent

            return if ((designCapacity > 0) && (currentCapacity > 0)) {
                context.getString(R.string.mah_value_with_percent, currentCapacity, getStringPercent(context, (currentCapacity * 100f) / designCapacity))
            } else null
        }

        BATTERY_PROPERTY_ENERGY_COUNTER -> {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            val voltage = batteryIntentHelper.voltage

            return if ((energy != Long.MIN_VALUE) && (voltage > 0)) {
                context.getString(R.string.mah_value, ((energy / 1000f) / voltage).toInt())
            } else  null
        }

        else -> return batteryIntentHelper.toString(context, field, appPrefs, false)
    }
