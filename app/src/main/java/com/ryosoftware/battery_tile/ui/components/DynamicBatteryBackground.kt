package com.ryosoftware.battery_tile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ryosoftware.battery_tile.ui.theme.AccentColors

@Composable
fun DynamicBatteryBackground(
    batteryLevel: Int,
    isCharging: Boolean,
    temperatureCelsius: Float,
    modifier: Modifier = Modifier,
) {
    val accentColor = when {
        temperatureCelsius > 45f -> AccentColors.temperatureRed
        temperatureCelsius > 35f -> AccentColors.warningAmber
        !isCharging && batteryLevel <= 20 -> AccentColors.warningAmber
        isCharging && batteryLevel >= 100 -> AccentColors.batteryGreen
        isCharging -> AccentColors.charging
        else -> MaterialTheme.colorScheme.primary
    }

    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(1000),
        label = "bgAccent",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        animatedAccent.copy(alpha = 0.25f),
                    ),
                ),
            ),
    )
}
