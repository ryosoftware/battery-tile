package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ryosoftware.battery_tile.Main.Companion.hasBatteryOptimizationBypassPermission
import com.ryosoftware.battery_tile.Main.Companion.hasPostNotificationsPermission
import com.ryosoftware.battery_tile.Main.Companion.requestBypassBatteryOptimizationPermission
import com.ryosoftware.battery_tile.Main.Companion.requestPostNotificationsPermission

class MainActivity : ComponentActivity() {

    private enum class Screen { Main, Selector, TileSettings, NotificationSettings, BarOverlaySettings, DebugLog, BatteryInfo, BatteryHistory }

    private var screen by mutableStateOf(Screen.Main)

    private var postNotificationsPermissionRequested = false
    private var batteryOptimizationsBypassPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val appPrefs = AppPreferences(this)

        val batteryOverlayPreferences = BatteryOverlayPreferences(this)

        cacheNotchCenter(batteryOverlayPreferences)

        BatteryTileTheme.glassEnabled = appPrefs.uiGlassEnabled
        BatteryTileTheme.themeMode = try {
            ExpressiveThemeMode.valueOf(appPrefs.uiThemeMode)
        } catch (_: IllegalArgumentException) {
            ExpressiveThemeMode.DYNAMIC
        }

        screen = if (!appPrefs.isFirstRun) Screen.Selector else Screen.Main

        if (appPrefs.batteryCapacityDesign == -1) appPrefs.batteryCapacityDesign = BatteryIntentHelper.getBatteryCapacityDesign(this)

        setContent {
            ActivityTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val batteryTilePreferences = remember { BatteryTilePreferences(context) }
                    val notifPrefs = remember { NotificationPreferences(context) }
                    val appPrefs = remember { AppPreferences(context) }

                    Crossfade(targetState = screen, animationSpec = tween(300)) {
                        when (it) {
                            Screen.Main -> {
                            MainScreen(
                                onSettings = {
                                    appPrefs.isFirstRun = false
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.Selector -> {
                            SettingsSelector(
                                appPrefs = appPrefs,
                                notifPrefs = notifPrefs,
                                overlayPrefs = batteryOverlayPreferences,
                                onTileSettings = { screen = Screen.TileSettings },
                                onNotificationSettings = { screen = Screen.NotificationSettings },
                                onBarOverlaySettings = { screen = Screen.BarOverlaySettings },
                                onDebugLog = { screen = Screen.DebugLog },
                                onBatteryInfo = { screen = Screen.BatteryInfo },
                                onBatteryHistory = { screen = Screen.BatteryHistory }
                            )
                        }

                        Screen.DebugLog -> {
                            BackHandler { screen = Screen.Selector }

                            DebugLogScreen(
                                onBack = { screen = Screen.Selector }
                            )
                        }

                        Screen.TileSettings -> {
                            BackHandler { screen = Screen.Selector }

                            TileSettingsScreen(
                                prefs = batteryTilePreferences,
                                onBack = {
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.NotificationSettings -> {
                            BackHandler { screen = Screen.Selector }

                            NotificationSettingsScreen(
                                prefs = batteryTilePreferences,
                                notifPrefs = notifPrefs,
                                onBack = {
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.BarOverlaySettings -> {
                            BackHandler { screen = Screen.Selector }

                            BatteryOverlaySettingsScreen(
                                prefs = batteryOverlayPreferences,
                                onBack = {
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.BatteryInfo -> {
                            BackHandler { screen = Screen.Selector }

                            BatteryInfoScreen(
                                batteryTilePreferences = batteryTilePreferences,
                                batteryOverlayPreferences = batteryOverlayPreferences,
                                appPrefs = appPrefs,
                                onBack = { screen = Screen.Selector }
                            )
                        }

                        Screen.BatteryHistory -> {
                            BackHandler { screen = Screen.Selector }

                            BatteryHistoryScreen(
                                appPrefs = appPrefs,
                                onBack = { screen = Screen.Selector }
                            )
                        }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun cacheNotchCenter(prefs: BatteryOverlayPreferences) {
        window.decorView.setOnApplyWindowInsetsListener { decorView, insetsInfo ->
            insetsInfo.displayCutout?.boundingRects?.firstOrNull()?.let { rect ->
                val location = IntArray(2)

                decorView.getLocationOnScreen(location)

                val notchCenterX = location[0] + rect.centerX()
                val notchCenterY = location[1] + rect.centerY()
                if ((notchCenterX >= 0) && (notchCenterY >= 0)) {
                    prefs.notchCenterX = notchCenterX
                    prefs.notchCenterY = notchCenterY
                    prefs.notchWidthPx = rect.width().coerceAtLeast(0)
                }
            }
            insetsInfo
        }
    }

    @SuppressLint("InlinedApi")
    private fun setForegroundServiceStatus() {
        val prefs = NotificationPreferences(this)
        val willRun = prefs.isNotificationEnabled

        if (willRun) {
            if ((!postNotificationsPermissionRequested) && (!hasPostNotificationsPermission())) {
                postNotificationsPermissionRequested = true
                requestPostNotificationsPermission()
            }

            if ((!batteryOptimizationsBypassPermissionRequested) && (!hasBatteryOptimizationBypassPermission())) {
                batteryOptimizationsBypassPermissionRequested = true
                requestBypassBatteryOptimizationPermission()
            }
        }

        NotificationService.runOrStop(this)
    }
    override fun onResume() {
        super.onResume()
        setForegroundServiceStatus()
    }
}
