package com.ryosoftware.battery_tile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object ExpressiveShapes {
    val extraSmall = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
        bottomEnd = 4.dp,
        bottomStart = 4.dp,
    )

    val small = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomEnd = 6.dp,
        bottomStart = 6.dp,
    )

    val medium = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 16.dp,
        bottomEnd = 8.dp,
        bottomStart = 12.dp,
    )

    val large = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 20.dp,
        bottomEnd = 12.dp,
        bottomStart = 16.dp,
    )

    val extraLarge = RoundedCornerShape(
        topStart = 36.dp,
        topEnd = 24.dp,
        bottomEnd = 16.dp,
        bottomStart = 20.dp,
    )

    val bottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
    )
}

val MaterialShapes = Shapes(
    extraSmall = ExpressiveShapes.extraSmall,
    small = ExpressiveShapes.small,
    medium = ExpressiveShapes.medium,
    large = ExpressiveShapes.large,
    extraLarge = ExpressiveShapes.extraLarge,
)
