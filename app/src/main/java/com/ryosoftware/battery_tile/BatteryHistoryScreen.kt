package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.BatteryRepository
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryScreen(
    appPrefs: AppPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BatteryRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val readings by repository.getAll().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write("\uFEFF".toByteArray(Charsets.UTF_8))
                    outputStream.write(buildCsv(context, readings, appPrefs).toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Toast.makeText(context, R.string.error_exporting_history, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (readings.size >= 2) {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val tempFile = File(context.cacheDir, "battery_history.csv")
                                    tempFile.outputStream().use { os ->
                                        os.write("\uFEFF".toByteArray(Charsets.UTF_8))
                                        os.write(buildCsv(context, readings, appPrefs).toByteArray(Charsets.UTF_8))
                                    }

                                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.file_provider", tempFile)

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.error_exporting_history, Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.share_history)
                            )
                        }
                        IconButton(onClick = {
                            saveLauncher.launch("battery_history.csv")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = stringResource(R.string.save_history)
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                repository.deleteAll()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.clear_history)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(stringResource(R.string.battery_level)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(stringResource(R.string.battery_temperature)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (readings.size < 2) {
                Text(
                    text = stringResource(R.string.no_history_data),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val displayReadings = readings.reversed()

                when (selectedTab) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.battery_level_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        BatteryLevelChart(context = context, readings = displayReadings)
                    }
                    1 -> {
                        Text(
                            text = stringResource(R.string.temperature_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        TemperatureChart(context = context, readings = displayReadings, appPrefs = appPrefs)
                    }
                }

                Spacer(Modifier.height(16.dp))

                val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, LocalLocale.current.platformLocale)
                val oldest = readings.last()
                val newest = readings.first()

                @SuppressLint("LocalContextResourcesRead")
                Text(
                    text = context.resources.getQuantityString(R.plurals.readings_count, readings.size, readings.size, dateFormat.format(Date(oldest.timestamp)), dateFormat.format(Date(newest.timestamp))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                @SuppressLint("LocalContextResourcesRead")
                Text(
                    text = context.resources.getQuantityString(R.plurals.history_retention_days, appPrefs.batteryHistoryWindow, appPrefs.batteryHistoryWindow),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun findDayChanges(readings: List<BatteryReading>): List<Int> {
    if (readings.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    var lastDay = -1
    val changes = mutableListOf<Int>()
    readings.forEachIndexed { index, reading ->
        cal.timeInMillis = reading.timestamp
        val day = cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 1000
        if (day != lastDay && lastDay != -1) {
            changes.add(index)
        }
        lastDay = day
    }
    return changes
}

private fun formatDateLabel(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
}

@Composable
fun BatteryLevelChart(
    context: Context,
    readings: List<BatteryReading>,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    val chartHeightDp = 300.dp
    val labelWidthDp = 40.dp
    val minWidthPerPoint = 12.dp
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val chartWidth = (minWidthPerPoint * readings.size).coerceAtLeast(screenWidth - labelWidthDp)
    val horizontalScrollState = rememberScrollState()

    val dayChanges = remember(readings) { findDayChanges(readings) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeightDp)
    ) {
        Canvas(
            modifier = Modifier
                .width(labelWidthDp)
                .height(chartHeightDp)
                .padding(top = 8.dp, bottom = 30.dp)
        ) {
            if (readings.size < 2) return@Canvas

            val height = size.height
            val paddingLeft = 8.dp.toPx()
            val paddingRight = 4.dp.toPx()
            val chartDrawWidth = size.width - paddingLeft - paddingRight

            for (i in 0..4) {
                val y = height * i / 4
                val value = 100 - (i * 25)
                drawContext.canvas.nativeCanvas.drawText(
                    "$value%",
                    paddingLeft + chartDrawWidth,
                    y + 4.dp.toPx(),
                    Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = Paint.Align.RIGHT
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(chartHeightDp)
            ) {
                if (readings.size < 2) return@Canvas

                val width = chartWidth.toPx()
                val height = size.height
                val paddingBottom = 30.dp.toPx()
                val paddingTop = 8.dp.toPx()
                val paddingRight = 8.dp.toPx()

                val chartDrawWidth = width - paddingRight
                val chartDrawHeight = height - paddingTop - paddingBottom

                for (i in 0..4) {
                    val y = paddingTop + (chartDrawHeight * i / 4)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                for (dayIndex in dayChanges) {
                    val x = chartDrawWidth * dayIndex / (readings.size - 1).coerceAtLeast(1)
                    drawLine(
                        color = gridColor,
                        start = Offset(x, paddingTop),
                        end = Offset(x, paddingTop + chartDrawHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatDateLabel(readings[dayIndex].timestamp),
                        x,
                        height - 4.dp.toPx(),
                        Paint().apply {
                            color = textColor.hashCode()
                            textSize = 9.sp.toPx()
                            textAlign = Paint.Align.CENTER
                        }
                    )
                }

                val path = Path()
                readings.forEachIndexed { index, reading ->
                    val x = chartDrawWidth * index / (readings.size - 1).coerceAtLeast(1)
                    val y = paddingTop + chartDrawHeight * (1f - reading.batteryLevel / 100f)

                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }

    if (horizontalScrollState.maxValue > 0) {
        Text(
            text = stringResource(R.string.chart_scroll_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun TemperatureChart(
    context: Context,
    readings: List<BatteryReading>,
    appPrefs: AppPreferences
) {
    val lineColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    val chartHeightDp = 300.dp
    val labelWidthDp = 50.dp
    val minWidthPerPoint = 12.dp
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val chartWidth = (minWidthPerPoint * readings.size).coerceAtLeast(screenWidth - labelWidthDp)
    val horizontalScrollState = rememberScrollState()

    val dayChanges = remember(readings) { findDayChanges(readings) }

    val temps = remember(readings) { readings.map { it.temperatureCelsius } }
    val minTemp = remember(temps) { (temps.min() - 5f).coerceAtLeast(0f) }
    val maxTemp = remember(temps) { (temps.max() + 5f).coerceAtMost(60f) }
    val tempRange = remember(minTemp, maxTemp) { maxTemp - minTemp }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeightDp)
    ) {
        Canvas(
            modifier = Modifier
                .width(labelWidthDp)
                .height(chartHeightDp)
                .padding(top = 8.dp, bottom = 30.dp)
        ) {
            if (readings.size < 2) return@Canvas

            val height = size.height
            val paddingLeft = 4.dp.toPx()
            val paddingRight = 4.dp.toPx()
            val chartDrawWidth = size.width - paddingLeft - paddingRight
            val chartDrawHeight = height

            for (i in 0..4) {
                val y = chartDrawHeight * i / 4
                val value = maxTemp - (tempRange * i / 4)
                drawContext.canvas.nativeCanvas.drawText(
                    appPrefs.temperatureUnit.toString(context, appPrefs.temperatureUnit.fromCelsius(value), false),
                    paddingLeft + chartDrawWidth,
                    y + 4.dp.toPx(),
                    Paint().apply {
                        color = textColor.hashCode()
                        textSize = 9.sp.toPx()
                        textAlign = Paint.Align.RIGHT
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(chartHeightDp)
            ) {
                if (readings.size < 2) return@Canvas

                val width = chartWidth.toPx()
                val height = size.height
                val paddingBottom = 30.dp.toPx()
                val paddingTop = 8.dp.toPx()
                val paddingRight = 8.dp.toPx()

                val chartDrawWidth = width - paddingRight
                val chartDrawHeight = height - paddingTop - paddingBottom

                for (i in 0..4) {
                    val y = paddingTop + (chartDrawHeight * i / 4)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                for (dayIndex in dayChanges) {
                    val x = chartDrawWidth * dayIndex / (readings.size - 1).coerceAtLeast(1)
                    drawLine(
                        color = gridColor,
                        start = Offset(x, paddingTop),
                        end = Offset(x, paddingTop + chartDrawHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatDateLabel(readings[dayIndex].timestamp),
                        x,
                        height - 4.dp.toPx(),
                        Paint().apply {
                            color = textColor.hashCode()
                            textSize = 9.sp.toPx()
                            textAlign = Paint.Align.CENTER
                        }
                    )
                }

                val path = Path()
                readings.forEachIndexed { index, reading ->
                    val x = chartDrawWidth * index / (readings.size - 1).coerceAtLeast(1)
                    val y = paddingTop + chartDrawHeight * (1f - (reading.temperatureCelsius - minTemp) / tempRange)

                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }

    if (horizontalScrollState.maxValue > 0) {
        Text(
            text = stringResource(R.string.chart_scroll_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun buildCsv(context: Context, readings: List<BatteryReading>, appPrefs: AppPreferences): String {
    fun formatBatteryStatus(context: Context, status: Int): String =
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.battery_status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.battery_status_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.battery_status_not_charging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_status_full)
            else -> context.getString(R.string.battery_status_unknown)
        }

    fun formatHealth(context: Context, health: Int): String =
        when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.battery_health_failure)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.battery_health_cold)
            else -> context.getString(R.string.battery_health_unknown)
        }

    fun formatPlugType(context: Context, plugType: Int): String =
        when (plugType) {
            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.battery_plug_ac)
            BatteryManager.BATTERY_PLUGGED_DOCK -> context.getString(R.string.battery_plug_dock)
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.battery_plug_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.battery_plug_wireless)
            else -> context.getString(R.string.not_available)
        }

    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
    val stringBuilder = StringBuilder()
    val temperatureUnit = appPrefs.temperatureUnit
    val yes = context.getString(R.string.yes)
    val no = context.getString(R.string.no)

    stringBuilder.appendLine(
        listOf(
            context.getString(R.string.csv_header_timestamp),
            context.getString(R.string.csv_header_battery_level),
            context.getString(R.string.csv_header_battery_status),
            context.getString(R.string.csv_header_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.csv_header_voltage),
            context.getString(R.string.csv_header_health),
            context.getString(R.string.csv_header_is_charging),
            context.getString(R.string.csv_header_plug_type)
        ).joinToString(";")
    )

    for (reading in readings) {
        stringBuilder.appendLine(
            listOf(
                dateFormat.format(Date(reading.timestamp)),
                reading.batteryLevel,
                formatBatteryStatus(context, reading.batteryStatus),
                temperatureUnit.fromCelsius(reading.temperatureCelsius),
                reading.voltage,
                formatHealth(context, reading.health),
                if (reading.isCharging) yes else no,
                formatPlugType(context, reading.plugType)
            ).joinToString(";")
        )
    }

    return stringBuilder.toString()
}
