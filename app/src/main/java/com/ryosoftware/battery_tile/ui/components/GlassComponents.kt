package com.ryosoftware.battery_tile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.BatteryTileTheme
import com.ryosoftware.battery_tile.ui.theme.AccentColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = MaterialTheme.shapes.medium as RoundedCornerShape,
    vibrant: Boolean = false,
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bgColor = if (BatteryTileTheme.glassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (vibrant) 0.88f else 0.82f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardColors = CardDefaults.cardColors(
        containerColor = bgColor,
    )

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = tween(120),
        label = "cardScale",
    )

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .scale(scale),
        shape = shape,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        interactionSource = interactionSource,
    ) {
        content()
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = MaterialTheme.shapes.small as RoundedCornerShape,
    vibrant: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = if (BatteryTileTheme.glassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (vibrant) 0.88f else 0.82f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun GlassBackgroundScrim(
    modifier: Modifier = Modifier,
) {
    if (BatteryTileTheme.glassEnabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AccentColors.glassScrim,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                )
        )
    }
}

@Composable
fun GlassGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    )
}
