package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.ui.components.GlassCard
import com.ryosoftware.battery_tile.ui.components.GlassGradientBackground
import com.ryosoftware.battery_tile.ui.components.SectionHeader
import com.ryosoftware.battery_tile.ui.theme.Spacing
import kotlin.math.roundToInt

private enum class ManualSliderField { BAR_HEIGHT, RING_RADIUS, RING_THICKNESS, RING_OFFSET_X, RING_OFFSET_Y }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOverlaySettingsScreen(
    prefs: BatteryOverlayPreferences,
    onBack: () -> Unit,
) {
    val count = prefs.segmentsCount

    val segmentPercents = remember {
        mutableStateListOf<Int>().apply {
            repeat(count) { index -> add(prefs.getSegmentStartLevel(index)) }
        }
    }
    val segmentColors = remember {
        mutableStateListOf<Color>().apply {
            repeat(count) { index -> add(prefs.getSegmentColor(index)) }
        }
    }

    var chargingColor by remember { mutableStateOf(prefs.chargingColor) }
    var barHeight by remember { mutableIntStateOf(prefs.barHeightPx) }

    var overlayMode by remember { mutableStateOf(prefs.overlayMode) }
    var ringRadius by remember { mutableIntStateOf(prefs.ringRadiusPx) }
    var ringThickness by remember { mutableIntStateOf(prefs.ringThicknessPx) }
    var ringOffsetX by remember { mutableIntStateOf(prefs.ringOffsetXPx) }
    var ringOffsetY by remember { mutableIntStateOf(prefs.ringOffsetYPx) }
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val density = LocalDensity.current.density
    val screenWidthPx = screenWidthDp * density
    val screenHeightPx = screenHeightDp * density
    var pickerForSegment by remember { mutableStateOf<Int?>(null) }
    var pickerForCharging by remember { mutableStateOf(false) }
    var manualField by remember { mutableStateOf<ManualSliderField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.battery_overlay_settings),
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl),
            ) {
                Spacer(Modifier.height(Spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    ModeCard(
                        label = stringResource(R.string.battery_overlay_mode_bar),
                        selected = overlayMode == BatteryOverlayMode.BAR,
                        onClick = {
                            overlayMode = BatteryOverlayMode.BAR
                            prefs.overlayMode = BatteryOverlayMode.BAR
                        },
                        modifier = Modifier.weight(1f),
                    )

                    ModeCard(
                        label = stringResource(R.string.battery_overlay_mode_ring),
                        selected = overlayMode == BatteryOverlayMode.RING,
                        onClick = {
                            overlayMode = BatteryOverlayMode.RING
                            prefs.overlayMode = BatteryOverlayMode.RING
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                if (overlayMode == BatteryOverlayMode.BAR) {
                    SliderRow(
                        label = stringResource(R.string.battery_overlay_bar_height),
                        value = barHeight.toFloat(),
                        onValueChange = { barHeight = it.roundToInt() },
                        onValueChangeFinished = { prefs.barHeightPx = barHeight },
                        valueRange = 1f..(10f * density),
                        steps = (10f * density).roundToInt() - 2,
                        displayValue = stringResource(R.string.battery_overlay_bar_height_value, barHeight),
                        onValueClick = { manualField = ManualSliderField.BAR_HEIGHT },
                    )
                } else {
                    SliderRow(
                        label = stringResource(R.string.battery_overlay_ring_radius),
                        value = ringRadius.toFloat(),
                        onValueChange = { ringRadius = it.roundToInt() },
                        onValueChangeFinished = { prefs.ringRadiusPx = ringRadius },
                        valueRange = 1f..(50f * density),
                        steps = (50f * density).roundToInt() - 2,
                        displayValue = stringResource(R.string.battery_overlay_bar_height_value, ringRadius),
                        onValueClick = { manualField = ManualSliderField.RING_RADIUS },
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    SliderRow(
                        label = stringResource(R.string.battery_overlay_ring_thickness),
                        value = ringThickness.toFloat(),
                        onValueChange = { ringThickness = it.roundToInt() },
                        onValueChangeFinished = { prefs.ringThicknessPx = ringThickness },
                        valueRange = 1f..(10f * density),
                        steps = (10f * density).roundToInt() - 2,
                        displayValue = stringResource(R.string.battery_overlay_bar_height_value, ringThickness),
                        onValueClick = { manualField = ManualSliderField.RING_THICKNESS },
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    SliderRow(
                        label = stringResource(R.string.battery_overlay_ring_offset_x),
                        value = ringOffsetX.toFloat(),
                        onValueChange = { ringOffsetX = it.roundToInt() },
                        onValueChangeFinished = { prefs.ringOffsetXPx = ringOffsetX },
                        valueRange = 0f..screenWidthPx,
                        steps = screenWidthPx.roundToInt() - 1,
                        displayValue = stringResource(R.string.battery_overlay_bar_height_value, ringOffsetX),
                        onValueClick = { manualField = ManualSliderField.RING_OFFSET_X },
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    SliderRow(
                        label = stringResource(R.string.battery_overlay_ring_offset_y),
                        value = ringOffsetY.toFloat(),
                        onValueChange = { ringOffsetY = it.roundToInt() },
                        onValueChangeFinished = { prefs.ringOffsetYPx = ringOffsetY },
                        valueRange = 0f..screenHeightPx,
                        steps = screenHeightPx.roundToInt() - 1,
                        displayValue = stringResource(R.string.battery_overlay_bar_height_value, ringOffsetY),
                        onValueClick = { manualField = ManualSliderField.RING_OFFSET_Y },
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                SectionHeader(
                    title = stringResource(R.string.battery_overlay_segments_and_colors),
                )

                Spacer(Modifier.height(Spacing.md))

                for (index in 0 until count) {
                    SegmentCard(
                        index = index,
                        percent = segmentPercents[index],
                        color = segmentColors[index],
                        showPercentSlider = index < count - 1,
                        percentRange = segmentPercentRange(segmentPercents, index),
                        onPercentChange = { percent ->
                            applySegmentChange(segmentPercents, index, percent)
                        },
                        onPercentChangeFinished = {
                            for (i in 0 until count) {
                                prefs.setSegmentStartLevel(i, segmentPercents[i])
                            }
                        },
                        onColorClick = { pickerForSegment = index },
                    )

                    Spacer(Modifier.height(Spacing.md))
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    vibrant = true,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickerForCharging = true }
                            .padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.battery_overlay_charging_color),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                        )

                        ColorSwatch(
                            color = chargingColor,
                            onClick = { pickerForCharging = true },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xxxl))
            }
        }
    }

    when (val segmentIndex = pickerForSegment) {
        null -> {}
        else -> ColorPickerDialog(
            initialColor = segmentColors[segmentIndex],
            onDismiss = { pickerForSegment = null },
            onConfirm = {
                segmentColors[segmentIndex] = it
                prefs.setSegmentColor(segmentIndex, it)
                pickerForSegment = null
            },
        )
    }

    if (pickerForCharging) {
        ColorPickerDialog(
            initialColor = chargingColor,
            onDismiss = { pickerForCharging = false },
            onConfirm = {
                chargingColor = it
                prefs.chargingColor = it
                pickerForCharging = false
            },
        )
    }

    when (manualField) {
        null -> {}
        ManualSliderField.BAR_HEIGHT -> IntegerInputDialog(
            title = stringResource(R.string.battery_overlay_bar_height),
            initial = barHeight,
            min = 1,
            max = (10f * density).roundToInt(),
            onDismiss = { manualField = null },
            onConfirm = {
                barHeight = it
                prefs.barHeightPx = it
                manualField = null
            },
        )
        ManualSliderField.RING_RADIUS -> IntegerInputDialog(
            title = stringResource(R.string.battery_overlay_ring_radius),
            initial = ringRadius,
            min = 1,
            max = (50f * density).roundToInt(),
            onDismiss = { manualField = null },
            onConfirm = {
                ringRadius = it
                prefs.ringRadiusPx = it
                manualField = null
            },
        )
        ManualSliderField.RING_THICKNESS -> IntegerInputDialog(
            title = stringResource(R.string.battery_overlay_ring_thickness),
            initial = ringThickness,
            min = 1,
            max = (10f * density).roundToInt(),
            onDismiss = { manualField = null },
            onConfirm = {
                ringThickness = it
                prefs.ringThicknessPx = it
                manualField = null
            },
        )
        ManualSliderField.RING_OFFSET_X -> IntegerInputDialog(
            title = stringResource(R.string.battery_overlay_ring_offset_x),
            initial = ringOffsetX,
            min = 0,
            max = screenWidthPx.roundToInt(),
            onDismiss = { manualField = null },
            onConfirm = {
                ringOffsetX = it
                prefs.ringOffsetXPx = it
                manualField = null
            },
        )
        ManualSliderField.RING_OFFSET_Y -> IntegerInputDialog(
            title = stringResource(R.string.battery_overlay_ring_offset_y),
            initial = ringOffsetY,
            min = 0,
            max = screenHeightPx.roundToInt(),
            onDismiss = { manualField = null },
            onConfirm = {
                ringOffsetY = it
                prefs.ringOffsetYPx = it
                manualField = null
            },
        )
    }
}

private fun applySegmentChange(percents: MutableList<Int>, index: Int, value: Int) {
    val v = value.coerceIn(0, 100)
    percents[index] = v

    for (j in index + 1 until percents.size) {
        val maxAllowed = v - (j - index)
        if (percents[j] > maxAllowed) percents[j] = maxAllowed
    }

    for (j in index - 1 downTo 0) {
        val minAllowed = v + (index - j)
        if (percents[j] < minAllowed) percents[j] = minAllowed
    }

    percents[percents.size - 1] = 0
}

private fun segmentPercentRange(percents: List<Int>, index: Int): ClosedFloatingPointRange<Float> {
    val min = if (index + 1 < percents.size) percents[index + 1] + 1 else 0
    val max = if (index > 0) percents[index - 1] - 1 else 100
    if (max <= min) return min.toFloat()..min.toFloat()
    return min.toFloat()..max.toFloat()
}

@Composable
private fun SegmentCard(
    index: Int,
    percent: Int,
    color: Color,
    showPercentSlider: Boolean,
    percentRange: ClosedFloatingPointRange<Float>,
    onPercentChange: (Int) -> Unit,
    onPercentChangeFinished: () -> Unit,
    onColorClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        vibrant = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ColorSwatch(
                    color = color,
                    onClick = onColorClick,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            if (showPercentSlider) {
                SliderRow(
                    label = stringResource(R.string.battery_overlay_segment_percent),
                    value = percent.toFloat(),
                    onValueChange = { onPercentChange(it.roundToInt().coerceIn(percentRange.start.toInt(), percentRange.endInclusive.toInt())) },
                    onValueChangeFinished = onPercentChangeFinished,
                    valueRange = percentRange,
                    steps = ((percentRange.endInclusive - percentRange.start).toInt() - 1).coerceAtLeast(0),
                    displayValue = stringResource(R.string.percent_value_integer, percent),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.battery_overlay_segment_percent),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = stringResource(R.string.percent_value_integer, 0),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Spacing.lg)
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val background =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.lg, horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueClick: () -> Unit = {},
) {
    Column {
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
                modifier = Modifier
                    .width(96.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .combinedClickable(
                        onLongClick = onValueClick,
                        onClick = {},
                    )
                    .padding(horizontal = Spacing.sm, vertical = 2.dp),
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegerInputDialog(
    title: String,
    initial: Int,
    min: Int,
    max: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val textFieldState = rememberTextFieldState(initialText = initial.toString())
    val value = textFieldState.text.toString().toIntOrNull()
    val error = (value == null) || (value < min) || (value > max)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                state = textFieldState,
                label = { Text(stringResource(R.string.battery_overlay_value)) },
                isError = error,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = !error,
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val textFieldState = rememberTextFieldState(initialText = colorToHex(initialColor))
    val currentColor = parseHexToColor(textFieldState.text.toString())
    val error = currentColor == null
    val context = LocalContext.current
    @SuppressLint("LocalContextResourcesRead")
    val presetColors = remember { context.resources.getIntArray(R.array.battery_bar_overlay_color_presets).map { Color(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_overlay_pick_color)) },
        text = {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    presetColors.forEach { color ->
                        val isSelected = color == currentColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    textFieldState.setTextAndPlaceCursorAtEnd(colorToHex(color))
                                },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                OutlinedTextField(
                    state = textFieldState,
                    label = { Text(stringResource(R.string.battery_overlay_color_hex)) },
                    isError = error,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { currentColor?.let(onConfirm) },
                enabled = !error,
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun colorToHex(color: Color): String {
    val rgb = color.toArgb() and 0xFFFFFF
    return String.format("#%06X", rgb)
}

private fun parseHexToColor(hex: String): Color? {
    val text = hex.trim().removePrefix("#")
    return when (text.length) {
        6 -> text.toLongOrNull(16)?.let { Color(0xFF000000L or it) }
        8 -> text.toLongOrNull(16)?.let { Color(it) }
        else -> null
    }
}