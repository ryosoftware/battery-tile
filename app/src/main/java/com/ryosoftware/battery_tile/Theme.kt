package com.ryosoftware.battery_tile

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ryosoftware.battery_tile.ui.theme.DarkExpressiveColors
import com.ryosoftware.battery_tile.ui.theme.ExpressiveTypography
import com.ryosoftware.battery_tile.ui.theme.LightExpressiveColors
import com.ryosoftware.battery_tile.ui.theme.MaterialShapes

enum class ExpressiveThemeMode {
    DYNAMIC,
    VIBRANT,
}

object BatteryTileTheme {
    var glassEnabled: Boolean = true
    var themeMode: ExpressiveThemeMode = ExpressiveThemeMode.DYNAMIC
}

@Composable
fun ActivityTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()

    val colorScheme = when {
        BatteryTileTheme.themeMode == ExpressiveThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkExpressiveColors
        else -> LightExpressiveColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = MaterialShapes,
        content = content,
    )
}
