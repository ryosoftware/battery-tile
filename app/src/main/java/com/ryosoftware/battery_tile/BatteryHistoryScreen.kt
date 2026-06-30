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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.BatteryRepository
import com.ryosoftware.battery_tile.data.ChargingSession
import com.ryosoftware.battery_tile.data.DischargeSession
import com.ryosoftware.battery_tile.data.ScreenState
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import java.io.OutputStream
import java.util.Calendar
import androidx.core.content.edit
import java.time.Instant
import java.time.ZoneId

private sealed interface SessionItem {
    val startTime: Long
    data class Charging(val session: com.ryosoftware.battery_tile.data.ChargingSession) : SessionItem {
        override val startTime get() = session.startTime
    }
    data class Discharge(val session: com.ryosoftware.battery_tile.data.DischargeSession) : SessionItem {
        override val startTime get() = session.startTime
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryScreen(
    appPrefs: AppPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BatteryRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val readings by repository.getAllBatteryReadings().collectAsState(initial = emptyList())
    val chargingSessions by repository.getAllChargingSessions().collectAsState(initial = emptyList())
    val screenStates by repository.getAllScreenStates().collectAsState(initial = emptyList())
    val dischargeSessions by repository.getAllDischargeSessions().collectAsState(initial = emptyList())
    val prefs = remember { context.getSharedPreferences("battery_history_prefs", Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableIntStateOf(prefs.getInt("selected-tab", 0)) }
    var showLevel by remember { mutableStateOf(prefs.getBoolean("show-level", true)) }
    var showTemperature by remember { mutableStateOf(prefs.getBoolean("show-temperature", true)) }

    LaunchedEffect(selectedTab) {
        prefs.edit { putInt("selected-tab", selectedTab) }
    }
    LaunchedEffect(showLevel) {
        prefs.edit { putBoolean("show-level", showLevel) }
    }
    LaunchedEffect(showTemperature) {
        prefs.edit { putBoolean("show-temperature", showTemperature) }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    buildExcel(context, readings, chargingSessions, dischargeSessions, screenStates, appPrefs, outputStream)
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
                                    val tempFile = File(context.cacheDir, "battery_history.xlsx")
                                    tempFile.outputStream().use { os ->
                                        buildExcel(context, readings, chargingSessions, dischargeSessions, screenStates, appPrefs, os)
                                    }

                                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.file_provider", tempFile)

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
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
                            saveLauncher.launch("battery_history.xlsx")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(stringResource(R.string.combined_graphs_tab)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(stringResource(R.string.charge_discharge_sessions_tab)) }
                )
            }

            when (selectedTab) {
                0 if readings.size < 2 -> {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.no_history_data),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                1 if chargingSessions.isEmpty() && dischargeSessions.isEmpty() -> {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.no_sessions),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Spacer(Modifier.height(8.dp))

                    when (selectedTab) {
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = showLevel,
                                    onClick = {
                                        val newShowLevel = !showLevel
                                        if (newShowLevel || showTemperature) showLevel = newShowLevel
                                    },
                                    label = { Text(stringResource(R.string.chart_show_level)) }
                                )
                                FilterChip(
                                    selected = showTemperature,
                                    onClick = {
                                        val newShowTemperature = !showTemperature
                                        if (newShowTemperature || showLevel) showTemperature =
                                            newShowTemperature
                                    },
                                    label = { Text(stringResource(R.string.chart_show_temperature)) }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            val displayReadings = readings.reversed()
                            if (showLevel && showTemperature) {
                                DualAxisChart(
                                    context = context,
                                    readings = displayReadings,
                                    screenStates = screenStates,
                                    thresholdTemperatureCelsius = remember {
                                        NotificationPreferences(context).getBatteryTemperatureThreshold(
                                            TemperatureUnit.CELSIUS
                                        )
                                    },
                                    appPrefs = appPrefs
                                )
                            } else if (showTemperature) {
                                TemperatureChart(
                                    context = context,
                                    readings = displayReadings,
                                    screenStates = screenStates,
                                    thresholdTemperatureCelsius = remember {
                                        NotificationPreferences(context).getBatteryTemperatureThreshold(
                                            TemperatureUnit.CELSIUS
                                        )
                                    },
                                    appPrefs = appPrefs
                                )
                            } else {
                                BatteryLevelChart(
                                    context = context,
                                    readings = displayReadings,
                                    screenStates = screenStates
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            val oldest = readings.last()
                            val newest = readings.first()

                            @SuppressLint("LocalContextResourcesRead")
                            Text(
                                text = context.resources.getQuantityString(
                                    R.plurals.readings_count,
                                    readings.size,
                                    readings.size,
                                    getStringDateTime(context, oldest.timestamp),
                                    getStringDateTime(context, newest.timestamp)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(4.dp))

                            @SuppressLint("LocalContextResourcesRead")
                            Text(
                                text = context.resources.getQuantityString(
                                    R.plurals.history_retention_days,
                                    appPrefs.batteryHistoryWindow,
                                    appPrefs.batteryHistoryWindow
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        1 -> {
                            CombinedSessionsTab(
                                context = context,
                                chargingSessions = chargingSessions,
                                dischargeSessions = dischargeSessions,
                                appPrefs = appPrefs
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun findDayChanges(readings: List<BatteryReading>): List<Long> {
    if (readings.size < 2) return emptyList()
    val start = readings.first().timestamp
    val end = readings.last().timestamp
    val firstMidnight = Calendar.getInstance().apply {
        timeInMillis = start
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= start) add(Calendar.DAY_OF_MONTH, 1)
    }
    val changes = mutableListOf<Long>()
    val cal = firstMidnight.clone() as Calendar
    while (cal.timeInMillis < end) {
        changes.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return changes
}

private fun findHourChanges(readings: List<BatteryReading>): List<Long> {
    if (readings.size < 2) return emptyList()
    val start = readings.first().timestamp
    val end = readings.last().timestamp
    val firstHour = Calendar.getInstance().apply {
        timeInMillis = start
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= start) add(Calendar.HOUR_OF_DAY, 1)
    }
    val changes = mutableListOf<Long>()
    val cal = firstHour.clone() as Calendar
    while (cal.timeInMillis < end) {
        changes.add(cal.timeInMillis)
        cal.add(Calendar.HOUR_OF_DAY, 1)
    }
    return changes
}

private fun formatHourLabel(timestamp: Long): String {
    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}

private fun formatDateLabel(timestamp: Long): String {
    val formatter = DateFormat.getDateInstance(DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}

private fun isScreenOnAt(timestamp: Long, screenStates: List<ScreenState>): Boolean {
    if (screenStates.isEmpty()) return true

    var lastState = true

    for (state in screenStates) {
        if (state.timestamp > timestamp) break
        lastState = state.screenOn
    }

    return lastState
}

private fun enrichReadingsWithScreenStates(
    readings: List<BatteryReading>,
    screenStates: List<ScreenState>
): List<BatteryReading> {
    if (readings.size < 2 || screenStates.isEmpty()) return readings

    val result = mutableListOf<BatteryReading>()

    for (i in readings.indices) {
        if (i > 0) {
            val prev = readings[i - 1]
            val curr = readings[i]
            val totalDuration = (curr.timestamp - prev.timestamp).toFloat()
            if (totalDuration > 0f) {
                val intermediateStates = screenStates.filter {
                    it.timestamp > prev.timestamp && it.timestamp < curr.timestamp
                }
                for (state in intermediateStates) {
                    val progress = (state.timestamp - prev.timestamp) / totalDuration
                    result.add(
                        BatteryReading(
                            timestamp = state.timestamp,
                            batteryLevel = (prev.batteryLevel + (curr.batteryLevel - prev.batteryLevel) * progress).toInt(),
                            batteryStatus = prev.batteryStatus,
                            temperatureCelsius = prev.temperatureCelsius + (curr.temperatureCelsius - prev.temperatureCelsius) * progress,
                            voltage = (prev.voltage + (curr.voltage - prev.voltage) * progress).toInt(),
                            health = prev.health,
                            isCharging = prev.isCharging,
                            plugType = prev.plugType
                        )
                    )
                }
            }
        }
        result.add(readings[i])
    }

    return result
}

private enum class AxisSide { LEFT, RIGHT }

private data class SeriesConfig(
    val label: String,
    val yValue: (BatteryReading) -> Float,
    val yLabels: DrawScope.(textColor: Color) -> Unit,
    val lineColor: Color,
    val labelWidthDp: Dp,
    val side: AxisSide = AxisSide.LEFT,
    val referenceLineNormalizedY: Float? = null,
    val referenceLineLabel: String? = null,
    val referenceLineColor: Color = Color.Unspecified,
    val segmentColorProvider: ((prev: BatteryReading, screenStates: List<ScreenState>) -> Color)? = null
)

@Composable
private fun UnifiedChart(
    readings: List<BatteryReading>,
    screenStates: List<ScreenState>,
    series: List<SeriesConfig>
) {
    if (readings.size < 2) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val screenOnColor = MaterialTheme.colorScheme.primary
    val screenOffColor = MaterialTheme.colorScheme.outline
    val screenOffShade = screenOffColor.copy(alpha = 0.08f)

    val chartHeightDp = 300.dp
    val leftLabelWidth = series.filter { it.side == AxisSide.LEFT }.maxOf { it.labelWidthDp }
    val rightLabelWidth = series.filter { it.side == AxisSide.RIGHT }.maxOfOrNull { it.labelWidthDp } ?: 0.dp
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val timeRangeMs = readings.last().timestamp - readings.first().timestamp
    val chartWidth = ((timeRangeMs / 3_600_000f) * 40f).dp.coerceAtLeast(screenWidth - leftLabelWidth - rightLabelWidth)
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(readings) {
        if (readings.size >= 2) {
            withFrameNanos { }
            horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
        }
    }

    val dayChanges = remember(readings) { findDayChanges(readings) }
    val hourChanges = remember(readings) { findHourChanges(readings) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(leftLabelWidth)
                    .height(chartHeightDp)
                    .padding(top = 8.dp, bottom = 30.dp)
            ) {
                if (readings.size < 2) return@Canvas
                val leftSeries = series.first { it.side == AxisSide.LEFT }
                leftSeries.yLabels(this, textColor)

                if (leftSeries.referenceLineNormalizedY != null && leftSeries.referenceLineLabel != null) {
                    val h = size.height
                    val y = h * (1f - leftSeries.referenceLineNormalizedY)
                    drawContext.canvas.nativeCanvas.drawText(
                        leftSeries.referenceLineLabel,
                        size.width - 4.dp.toPx(),
                        y + 4.dp.toPx(),
                        Paint().apply {
                            color = leftSeries.referenceLineColor.hashCode()
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

                    val firstTimestamp = readings.first().timestamp
                    val timeRange = readings.last().timestamp - firstTimestamp
                    fun timeToX(timestamp: Long): Float =
                        chartDrawWidth * (timestamp - firstTimestamp).toFloat() / timeRange.toFloat()

                    for (i in 0..4) {
                        val y = paddingTop + (chartDrawHeight * i / 4)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    for (hourTimestamp in hourChanges) {
                        val x = timeToX(hourTimestamp)
                        drawLine(
                            color = gridColor,
                            start = Offset(x, paddingTop),
                            end = Offset(x, paddingTop + chartDrawHeight),
                            strokeWidth = 0.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                        )
                        val cal = Calendar.getInstance().apply { timeInMillis = hourTimestamp }
                        if (cal.get(Calendar.HOUR_OF_DAY) != 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                formatHourLabel(hourTimestamp),
                                x,
                                height - 4.dp.toPx(),
                                Paint().apply {
                                    color = textColor.hashCode()
                                    textSize = 8.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                }
                            )
                        }
                    }

                    for (dayTimestamp in dayChanges) {
                        val x = timeToX(dayTimestamp)
                        drawLine(
                            color = gridColor,
                            start = Offset(x, paddingTop),
                            end = Offset(x, paddingTop + chartDrawHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            formatDateLabel(dayTimestamp),
                            x,
                            height - 4.dp.toPx(),
                            Paint().apply {
                                color = textColor.hashCode()
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }

                    readings.forEachIndexed { index, reading ->
                        if (index == 0) return@forEachIndexed
                        val prev = readings[index - 1]
                        val x1 = timeToX(prev.timestamp)
                        if (!isScreenOnAt(prev.timestamp, screenStates)) {
                            drawRect(
                                color = screenOffShade,
                                topLeft = Offset(x1, paddingTop),
                                size = androidx.compose.ui.geometry.Size(
                                    timeToX(reading.timestamp) - x1,
                                    chartDrawHeight
                                )
                            )
                        }
                    }

                    for (config in series) {
                        readings.forEachIndexed { index, reading ->
                            if (index == 0) return@forEachIndexed
                            val prev = readings[index - 1]
                            val x1 = timeToX(prev.timestamp)
                            val y1 = paddingTop + chartDrawHeight * (1f - config.yValue(prev))
                            val x2 = timeToX(reading.timestamp)
                            val y2 = paddingTop + chartDrawHeight * (1f - config.yValue(reading))
                            val color = config.segmentColorProvider?.invoke(prev, screenStates) ?: config.lineColor
                            drawLine(
                                color = color,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    for (config in series) {
                        val refY = config.referenceLineNormalizedY
                        if (refY != null) {
                            val y = paddingTop + chartDrawHeight * (1f - refY)
                            drawLine(
                                color = config.referenceLineColor,
                                start = Offset(0f, y),
                                end = Offset(width - paddingRight, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                            )
                        }
                    }
                }
            }

            if (rightLabelWidth > 0.dp) {
                Canvas(
                    modifier = Modifier
                        .width(rightLabelWidth)
                        .height(chartHeightDp)
                        .padding(top = 8.dp, bottom = 30.dp)
                ) {
                    if (readings.size < 2) return@Canvas
                    val rightSeries = series.first { it.side == AxisSide.RIGHT }
                    rightSeries.yLabels(this, textColor)

                    if (rightSeries.referenceLineNormalizedY != null && rightSeries.referenceLineLabel != null) {
                        val h = size.height
                        val y = h * (1f - rightSeries.referenceLineNormalizedY)
                        drawContext.canvas.nativeCanvas.drawText(
                            rightSeries.referenceLineLabel,
                            4.dp.toPx(),
                            y + 4.dp.toPx(),
                            Paint().apply {
                                color = rightSeries.referenceLineColor.hashCode()
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.LEFT
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (config in series) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawLine(color = config.lineColor, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx())
                }
                Text(
                    text = config.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                )
            }
        }
    }

    if (horizontalScrollState.maxValue > 0) {
        Text(
            text = stringResource(R.string.chart_scroll_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun BatteryLevelChart(
    context: Context,
    readings: List<BatteryReading>,
    screenStates: List<ScreenState>,
) {
    val enrichedReadings = remember(readings, screenStates) { enrichReadingsWithScreenStates(readings, screenStates) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    UnifiedChart(
        readings = enrichedReadings,
        screenStates = screenStates,
        series = listOf(
            SeriesConfig(
                label = stringResource(R.string.chart_show_level),
                yValue = { reading -> reading.batteryLevel / 100f },
                yLabels = { textColor ->
                    for (i in 0..4) {
                        val y = size.height * i / 4
                        val value = 100 - (i * 25)
                        drawContext.canvas.nativeCanvas.drawText(
                            context.getString(R.string.percent_value_integer, value),
                            8.dp.toPx() + (size.width - 12.dp.toPx()),
                            y + 4.dp.toPx(),
                            Paint().apply {
                                color = textColor.hashCode()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                            }
                        )
                    }
                },
                lineColor = primaryColor,
                labelWidthDp = 40.dp,
                side = AxisSide.LEFT,
                segmentColorProvider = { prev, states ->
                    if (isScreenOnAt(prev.timestamp, states)) primaryColor else outlineColor
                }
            )
        )
    )
}

@Composable
fun TemperatureChart(
    context: Context,
    readings: List<BatteryReading>,
    screenStates: List<ScreenState>,
    thresholdTemperatureCelsius: Float?,
    appPrefs: AppPreferences
) {
    val enrichedReadings = remember(readings, screenStates) { enrichReadingsWithScreenStates(readings, screenStates) }
    val temps = remember(enrichedReadings) { enrichedReadings.map { it.temperatureCelsius } }
    val minTemp = remember(temps) { (temps.min() - 5f).coerceAtLeast(0f) }
    val maxTemp = remember(temps) { (temps.max() + 5f).coerceAtMost(60f) }
    val tempRange = remember(minTemp, maxTemp) { maxTemp - minTemp }

    val thresholdNormalizedY = remember(thresholdTemperatureCelsius, minTemp, tempRange) {
        if (thresholdTemperatureCelsius != null && tempRange > 0f) {
            ((thresholdTemperatureCelsius - minTemp) / tempRange).coerceIn(0f, 1f)
        } else null
    }
    val thresholdLabel = remember(thresholdTemperatureCelsius, appPrefs.temperatureUnit) {
        if (thresholdTemperatureCelsius != null) {
            appPrefs.temperatureUnit.toString(context, appPrefs.temperatureUnit.fromCelsius(thresholdTemperatureCelsius), false)
        } else null
    }

    UnifiedChart(
        readings = enrichedReadings,
        screenStates = screenStates,
        series = listOf(
            SeriesConfig(
                label = stringResource(R.string.chart_show_temperature),
                yValue = { reading -> (reading.temperatureCelsius - minTemp) / tempRange },
                yLabels = { textColor ->
                    for (i in 0..4) {
                        val y = size.height * i / 4
                        val value = maxTemp - (tempRange * i / 4)
                        drawContext.canvas.nativeCanvas.drawText(
                            appPrefs.temperatureUnit.toString(context, appPrefs.temperatureUnit.fromCelsius(value), false),
                            4.dp.toPx() + (size.width - 8.dp.toPx()),
                            y + 4.dp.toPx(),
                            Paint().apply {
                                color = textColor.hashCode()
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                            }
                        )
                    }
                },
                lineColor = MaterialTheme.colorScheme.error,
                labelWidthDp = 50.dp,
                side = AxisSide.LEFT,
                referenceLineNormalizedY = thresholdNormalizedY,
                referenceLineLabel = thresholdLabel,
                referenceLineColor = MaterialTheme.colorScheme.error
            )
        )
    )
}

@Composable
private fun DualAxisChart(
    context: Context,
    readings: List<BatteryReading>,
    screenStates: List<ScreenState>,
    thresholdTemperatureCelsius: Float?,
    appPrefs: AppPreferences
) {
    val enrichedReadings = remember(readings, screenStates) { enrichReadingsWithScreenStates(readings, screenStates) }
    if (enrichedReadings.size < 2) return

    val temps = remember(enrichedReadings) { enrichedReadings.map { it.temperatureCelsius } }
    val minTemp = remember(temps) { (temps.min() - 5f).coerceAtLeast(0f) }
    val maxTemp = remember(temps) { (temps.max() + 5f).coerceAtMost(60f) }
    val tempRange = remember(minTemp, maxTemp) { maxTemp - minTemp }

    val thresholdNormalizedY = remember(thresholdTemperatureCelsius, minTemp, tempRange) {
        if (thresholdTemperatureCelsius != null && tempRange > 0f) {
            ((thresholdTemperatureCelsius - minTemp) / tempRange).coerceIn(0f, 1f)
        } else null
    }
    val thresholdLabel = remember(thresholdTemperatureCelsius, appPrefs.temperatureUnit) {
        if (thresholdTemperatureCelsius != null) {
            appPrefs.temperatureUnit.toString(context, appPrefs.temperatureUnit.fromCelsius(thresholdTemperatureCelsius), false)
        } else null
    }

    val screenOnColor = MaterialTheme.colorScheme.primary
    val screenOffColor = MaterialTheme.colorScheme.outline

    UnifiedChart(
        readings = enrichedReadings,
        screenStates = screenStates,
        series = listOf(
            SeriesConfig(
                label = stringResource(R.string.chart_show_level),
                yValue = { reading -> reading.batteryLevel / 100f },
                yLabels = { textColor ->
                    for (i in 0..4) {
                        val y = size.height * i / 4
                        val value = 100 - (i * 25)
                        drawContext.canvas.nativeCanvas.drawText(
                            context.getString(R.string.percent_value_integer, value),
                            size.width - 4.dp.toPx(),
                            y + 4.dp.toPx(),
                            Paint().apply {
                                color = textColor.hashCode()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                            }
                        )
                    }
                },
                lineColor = MaterialTheme.colorScheme.primary,
                labelWidthDp = 40.dp,
                side = AxisSide.LEFT,
                segmentColorProvider = { prev, _ ->
                    if (isScreenOnAt(prev.timestamp, screenStates)) screenOnColor else screenOffColor
                }
            ),
            SeriesConfig(
                label = stringResource(R.string.chart_show_temperature),
                yValue = { reading -> (reading.temperatureCelsius - minTemp) / tempRange },
                yLabels = { textColor ->
                    for (i in 0..4) {
                        val y = size.height * i / 4
                        val value = maxTemp - (tempRange * i / 4)
                        drawContext.canvas.nativeCanvas.drawText(
                            appPrefs.temperatureUnit.toString(context, appPrefs.temperatureUnit.fromCelsius(value), false),
                            4.dp.toPx(),
                            y + 4.dp.toPx(),
                            Paint().apply {
                                color = textColor.hashCode()
                                textSize = 9.sp.toPx()
                                textAlign = Paint.Align.LEFT
                            }
                        )
                    }
                },
                lineColor = MaterialTheme.colorScheme.error,
                labelWidthDp = 50.dp,
                side = AxisSide.RIGHT,
                referenceLineNormalizedY = thresholdNormalizedY,
                referenceLineLabel = thresholdLabel,
                referenceLineColor = MaterialTheme.colorScheme.error
            )
        )
    )
}

@Composable
fun CombinedSessionsTab(
    context: Context,
    chargingSessions: List<ChargingSession>,
    dischargeSessions: List<DischargeSession>,
    appPrefs: AppPreferences
) {
    val combined = remember(chargingSessions, dischargeSessions) {
        (chargingSessions.map { SessionItem.Charging(it) } +
         dischargeSessions.map { SessionItem.Discharge(it) })
            .sortedByDescending { it.startTime }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        combined.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when (item) {
                        is SessionItem.Charging -> ChargingSessionCard(item.session, context, appPrefs)
                        is SessionItem.Discharge -> DischargeSessionCard(item.session, context, appPrefs)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHeaderRow(
    dotColor: Color,
    label: String,
    labelColor: Color,
    isOngoing: Boolean,
    ongoingLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = dotColor, radius = size.minDimension / 2f)
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
        if (isOngoing) {
            Text(
                text = ongoingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DateTimeRow(context: Context, startTime: Long, endTime: Long?) {
    fun isSameDay(time1: Long, time2: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val date1 = Instant.ofEpochMilli(time1).atZone(zone).toLocalDate()
        val date2 = Instant.ofEpochMilli(time2).atZone(zone).toLocalDate()

        return date1 == date2
    }

    fun getIntervalAtSameDayString(context: Context, startTimeInMillis: Long, endTimeInMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = startTimeInMillis }
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM)
        val date = dateFormat.format(calendar.time)
        val startTime = timeFormat.format(calendar.time)
        val startHour = context.resources.getQuantityString(R.plurals.time, calendar.get(Calendar.HOUR_OF_DAY), startTime)
        calendar.apply { timeInMillis = endTimeInMillis }
        val endTime = timeFormat.format(calendar.time)
        val endHour = context.resources.getQuantityString(R.plurals.time, calendar.get(Calendar.HOUR_OF_DAY), endTime)

        return context.getString(R.string.from_time_to_time_on_date, startHour, endHour, date)
    }

    fun getIntervalAtDifferentDayString(context: Context, startTimeInMillis: Long, endTimeInMillis: Long): String =
        context.getString(R.string.from_date_to_date, getStringDateTime(context, startTimeInMillis), getStringDateTime(context, endTimeInMillis))

    Spacer(Modifier.height(4.dp))

    Text(
        text = if ((endTime != null) && (isSameDay(startTime, endTime))) getIntervalAtSameDayString(context, startTime, endTime)
               else if (endTime != null) getIntervalAtDifferentDayString(context, startTime, endTime)
               else getStringDateTime(context, startTime),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun BatteryLevelRow(startLevel: Int, endLevel: Int?) {
    Spacer(Modifier.height(4.dp))
    val endLevelText = endLevel?.let { stringResource(R.string.percent_value_integer, it) }
        ?: stringResource(R.string.battery_level_unknown)
    Text(
        text = stringResource(R.string.battery_level_from_to,
            stringResource(R.string.percent_value_integer, startLevel), endLevelText),
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun DurationRow(context: Context, durationMinutes: Long?, labelResId: Int) {
    if (durationMinutes != null) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.label_and_value,
                    stringResource(labelResId),
                    getStringTimeFromInterval(context, durationMinutes * 60_000L)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TemperatureRow(
    context: Context,
    minTemperatureCelsius: Float?,
    maxTemperatureCelsius: Float?,
    avgTemperatureCelsius: Float?,
    tempUnit: TemperatureUnit,
    labelResId: Int
) {
    if (avgTemperatureCelsius != null) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.label_and_value,
                    stringResource(labelResId),
                    stringResource(
                        R.string.charging_temperature_min_max_avg,
                        tempUnit.toString(context, tempUnit.fromCelsius(minTemperatureCelsius!!), false),
                        tempUnit.toString(context, tempUnit.fromCelsius(maxTemperatureCelsius!!), false),
                        tempUnit.toString(context, tempUnit.fromCelsius(avgTemperatureCelsius), false)
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChargingSessionCard(session: ChargingSession, context: Context, appPrefs: AppPreferences) {
    SessionHeaderRow(
        dotColor = Color(0xFF4CAF50),
        label = stringResource(R.string.session_charging_label),
        labelColor = Color(0xFF4CAF50),
        isOngoing = session.endTime == null,
        ongoingLabel = stringResource(R.string.charging_session_ongoing)
    )
    DateTimeRow(context, session.startTime, session.endTime)
    BatteryLevelRow(session.startLevel, session.endLevel)
    DurationRow(context, session.durationMinutes, R.string.charging_session_duration)

    if (session.endLevel != null) {
        val delta = session.endLevel - session.startLevel
        val speedPerHour = if (session.durationMinutes != null && session.durationMinutes > 0) {
            delta.toFloat() / (session.durationMinutes / 60f)
        } else null

        if (speedPerHour != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.label_and_value,
                        stringResource(R.string.charging_session_speed),
                        stringResource(R.string.percent_per_hour, getStringPercent(context, speedPerHour))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    TemperatureRow(
        context,
        session.minTemperatureCelsius,
        session.maxTemperatureCelsius,
        session.avgTemperatureCelsius,
        appPrefs.temperatureUnit,
        R.string.charging_session_temperature
    )

    if (session.chargedTimeStamp != null) {
        val endTime = session.endTime ?: System.currentTimeMillis()
        val diffMs = endTime - session.chargedTimeStamp
        val wastedMin = diffMs / 60_000L
        if (wastedMin > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.could_have_unplugged_before,
                    getStringDateTime(context, session.chargedTimeStamp),
                    getStringTimeFromInterval(context, diffMs)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun DischargeSessionCard(session: DischargeSession, context: Context, appPrefs: AppPreferences) {
    SessionHeaderRow(
        dotColor = Color(0xFF2196F3),
        label = stringResource(R.string.session_discharge_label),
        labelColor = Color(0xFF2196F3),
        isOngoing = session.endTime == null,
        ongoingLabel = stringResource(R.string.discharge_session_ongoing)
    )
    DateTimeRow(context, session.startTime, session.endTime)
    BatteryLevelRow(session.startLevel, session.endLevel)
    DurationRow(context, session.durationMinutes, R.string.discharge_session_duration)

    if (session.screenOnTimeMinutes != null) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.label_and_value,
                    stringResource(R.string.discharge_session_screen_on_time),
                    getStringTimeFromInterval(context, session.screenOnTimeMinutes * 60_000L)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    TemperatureRow(
        context,
        session.minTemperatureCelsius,
        session.maxTemperatureCelsius,
        session.avgTemperatureCelsius,
        appPrefs.temperatureUnit,
        R.string.discharge_session_temperature
    )
}

private fun buildExcel(
    context: Context,
    batteryReadings: List<BatteryReading>,
    chargingSessions: List<ChargingSession>,
    dischargeSessions: List<DischargeSession>,
    screenStates: List<ScreenState>,
    appPrefs: AppPreferences,
    outputStream: OutputStream
) {
    fun formatBatteryStatus(status: Int): String =
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.battery_status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.battery_status_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.battery_status_not_charging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_status_full)
            else -> context.getString(R.string.battery_status_unknown)
        }

    fun formatHealth(health: Int): String =
        when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.battery_health_failure)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.battery_health_cold)
            else -> context.getString(R.string.battery_health_unknown)
        }

    fun formatPlugType(plugType: Int): String =
        when (plugType) {
            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.battery_plug_ac)
            BatteryManager.BATTERY_PLUGGED_DOCK -> context.getString(R.string.battery_plug_dock)
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.battery_plug_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.battery_plug_wireless)
            else -> context.getString(R.string.not_available)
        }

    fun getColumnWidth(text: String) = (text.length + 2) * 256

    XSSFWorkbook().use { workbook ->
        val temperatureUnit = appPrefs.temperatureUnit

        val dateTimeStyle = workbook.createCellStyle().apply {
            dataFormat = 22
        }
        val durationTimeStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.creationHelper.createDataFormat().getFormat("[h]:mm:ss")
        }

        val batteryReadingsSheet = workbook.createSheet(context.getString(R.string.battery_level))
        val batteryReadingsHeaders = listOf(
            context.getString(R.string.excel_header_timestamp),
            context.getString(R.string.excel_header_battery_level),
            context.getString(R.string.excel_header_battery_status),
            context.getString(R.string.excel_header_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.excel_header_voltage),
            context.getString(R.string.excel_header_health),
            context.getString(R.string.excel_header_is_charging),
            context.getString(R.string.excel_header_plug_type),
            context.getString(R.string.excel_header_screen_on)
        )

        val batteryReadingsHeaderRow = batteryReadingsSheet.createRow(0)
        batteryReadingsHeaders.forEachIndexed { index, header ->
            batteryReadingsHeaderRow.createCell(index).setCellValue(header)
            batteryReadingsSheet.setColumnWidth(index, getColumnWidth(header))
        }

        val enrichedBatteryReadings = enrichReadingsWithScreenStates(batteryReadings, screenStates)

        for ((rowIndex, reading) in enrichedBatteryReadings.withIndex()) {
            val batteryReadingsBodyRow = batteryReadingsSheet.createRow(rowIndex + 1)
            batteryReadingsBodyRow.createCell(0).apply { setCellValue(Date(reading.timestamp)); cellStyle = dateTimeStyle }
            batteryReadingsBodyRow.createCell(1).setCellValue(reading.batteryLevel.toDouble())
            batteryReadingsBodyRow.createCell(2).setCellValue(formatBatteryStatus(reading.batteryStatus))
            batteryReadingsBodyRow.createCell(3).setCellValue(temperatureUnit.fromCelsius(reading.temperatureCelsius).toDouble())
            batteryReadingsBodyRow.createCell(4).setCellValue(reading.voltage.toDouble())
            batteryReadingsBodyRow.createCell(5).setCellValue(formatHealth(reading.health))
            batteryReadingsBodyRow.createCell(6).setCellValue(reading.isCharging)
            batteryReadingsBodyRow.createCell(7).setCellValue(formatPlugType(reading.plugType))
            batteryReadingsBodyRow.createCell(8).setCellValue(isScreenOnAt(reading.timestamp, screenStates))
        }

        val chargingSessionsSheet = workbook.createSheet(context.getString(R.string.excel_charging_sessions_tab))
        val chargingSessionsHeaders = listOf(
            context.getString(R.string.excel_header_start_timestamp),
            context.getString(R.string.excel_header_end_timestamp),
            context.getString(R.string.charging_session_duration),
            context.getString(R.string.excel_header_battery_start_level),
            context.getString(R.string.excel_header_battery_end_level),
            context.getString(R.string.excel_header_session_speed),
            context.getString(R.string.excel_header_min_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.excel_header_max_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.excel_header_avg_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.charging_session_plug_type),
            context.getString(R.string.excel_header_charged_timestamp)
        )

        val chargingSessionsHeaderRow = chargingSessionsSheet.createRow(0)
        chargingSessionsHeaders.forEachIndexed { index, header ->
            chargingSessionsHeaderRow.createCell(index).setCellValue(header)
            chargingSessionsSheet.setColumnWidth(index, getColumnWidth(header))
        }

        for ((rowIndex, session) in chargingSessions.withIndex()) {
            val chargingSessionsBodyRow = chargingSessionsSheet.createRow(rowIndex + 1)

            chargingSessionsBodyRow.createCell(0).apply { setCellValue(Date(session.startTime)); cellStyle = dateTimeStyle }
            if (session.endTime != null)
                chargingSessionsBodyRow.createCell(1).apply { setCellValue(Date(session.endTime)); cellStyle = dateTimeStyle }

            if (session.durationMinutes != null)
                chargingSessionsBodyRow.createCell(2).apply {
                    setCellValue(session.durationMinutes / 1440.0)
                    cellStyle = durationTimeStyle
                }

            chargingSessionsBodyRow.createCell(3).setCellValue(session.startLevel.toDouble())
            if (session.endLevel != null) chargingSessionsBodyRow.createCell(4).setCellValue(session.endLevel.toDouble())

            if (session.endLevel != null && session.durationMinutes != null && session.durationMinutes > 0) {
                val delta = session.endLevel - session.startLevel
                val speedPerHour = delta.toFloat() / (session.durationMinutes / 60f)
                chargingSessionsBodyRow.createCell(5).setCellValue(speedPerHour.toDouble())
            }

            if (session.minTemperatureCelsius != null) chargingSessionsBodyRow.createCell(6).setCellValue(temperatureUnit.fromCelsius(session.minTemperatureCelsius).toDouble())
            if (session.maxTemperatureCelsius != null) chargingSessionsBodyRow.createCell(7).setCellValue(temperatureUnit.fromCelsius(session.maxTemperatureCelsius).toDouble())
            if (session.avgTemperatureCelsius != null) chargingSessionsBodyRow.createCell(8).setCellValue(temperatureUnit.fromCelsius(session.avgTemperatureCelsius).toDouble())

            chargingSessionsBodyRow.createCell(9).setCellValue(formatPlugType(session.plugType))
            if (session.chargedTimeStamp != null) chargingSessionsBodyRow.createCell(10).apply { setCellValue(Date(session.chargedTimeStamp)); cellStyle = dateTimeStyle }
        }

        val dischargeSessionsSheet = workbook.createSheet(context.getString(R.string.excel_discharge_sessions_tab))
        val dischargeSessionsHeaders = listOf(
            context.getString(R.string.excel_header_start_timestamp),
            context.getString(R.string.excel_header_end_timestamp),
            context.getString(R.string.discharge_session_duration),
            context.getString(R.string.excel_header_battery_start_level),
            context.getString(R.string.excel_header_battery_end_level),
            context.getString(R.string.excel_header_screen_on_time),
            context.getString(R.string.excel_header_min_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.excel_header_max_temperature, temperatureUnit.toString(context)),
            context.getString(R.string.excel_header_avg_temperature, temperatureUnit.toString(context))
        )

        val dischargeSessionsHeaderRow = dischargeSessionsSheet.createRow(0)
        dischargeSessionsHeaders.forEachIndexed { index, header ->
            dischargeSessionsHeaderRow.createCell(index).setCellValue(header)
            dischargeSessionsSheet.setColumnWidth(index, getColumnWidth(header))
        }

        for ((rowIndex, session) in dischargeSessions.withIndex()) {
            val dischargeSessionsBodyRow = dischargeSessionsSheet.createRow(rowIndex + 1)

            dischargeSessionsBodyRow.createCell(0).apply { setCellValue(Date(session.startTime)); cellStyle = dateTimeStyle }
            if (session.endTime != null)
                dischargeSessionsBodyRow.createCell(1).apply { setCellValue(Date(session.endTime)); cellStyle = dateTimeStyle }

            if (session.durationMinutes != null)
                dischargeSessionsBodyRow.createCell(2).apply {
                    setCellValue(session.durationMinutes / 1440.0)
                    cellStyle = durationTimeStyle
                }

            dischargeSessionsBodyRow.createCell(3).setCellValue(session.startLevel.toDouble())
            if (session.endLevel != null) dischargeSessionsBodyRow.createCell(4).setCellValue(session.endLevel.toDouble())

            if (session.screenOnTimeMinutes != null)
                dischargeSessionsBodyRow.createCell(5).apply {
                    setCellValue(session.screenOnTimeMinutes / 1440.0)
                    cellStyle = durationTimeStyle
                }

            if (session.minTemperatureCelsius != null) dischargeSessionsBodyRow.createCell(6).setCellValue(temperatureUnit.fromCelsius(session.minTemperatureCelsius).toDouble())
            if (session.maxTemperatureCelsius != null) dischargeSessionsBodyRow.createCell(7).setCellValue(temperatureUnit.fromCelsius(session.maxTemperatureCelsius).toDouble())
            if (session.avgTemperatureCelsius != null) dischargeSessionsBodyRow.createCell(8).setCellValue(temperatureUnit.fromCelsius(session.avgTemperatureCelsius).toDouble())
        }

        val screenStatesSheet = workbook.createSheet(context.getString(R.string.screen_states_tab))
        val screenStatesHeaders = listOf(
            context.getString(R.string.excel_header_timestamp),
            context.getString(R.string.excel_header_screen_on)
        )

        val screenStatesHeaderRow = screenStatesSheet.createRow(0)
        screenStatesHeaders.forEachIndexed { index, header ->
            screenStatesHeaderRow.createCell(index).setCellValue(header)
            screenStatesSheet.setColumnWidth(index, getColumnWidth(header))
        }

        for ((rowIndex, state) in screenStates.withIndex()) {
            val screenStatesBodyRow = screenStatesSheet.createRow(rowIndex + 1)
            screenStatesBodyRow.createCell(0).apply { setCellValue(Date(state.timestamp)); cellStyle = dateTimeStyle }
            screenStatesBodyRow.createCell(1).setCellValue(state.screenOn)
        }

        workbook.write(outputStream)
    }
}

fun getStringTimeFromInterval(context: Context, interval: Long): String {
    val totalMinutes = interval / 60_000
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    return if (days > 0) {
        context.getString(R.string.days_and_hours_and_minutes, days, hours, minutes)
    } else if (hours > 0) {
        context.getString(R.string.hours_and_minutes, hours, minutes)
    } else {
        context.getString(R.string.minutes, minutes)
    }
}

fun getStringPercent(context: Context, percent: Float?): String {
    if (percent == null) return context.getString(R.string.not_available)

    val hasNoDecimals = percent % 1f == 0f

    return if (hasNoDecimals) context.getString(R.string.percent_value_integer, percent.toInt())
    else context.getString(R.string.percent_value_float, percent)
}

