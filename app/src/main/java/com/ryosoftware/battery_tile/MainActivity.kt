package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ryosoftware.battery_tile.Main.Companion.hasBatteryOptimizationBypassPermission
import com.ryosoftware.battery_tile.Main.Companion.hasPostNotificationsPermission
import com.ryosoftware.battery_tile.Main.Companion.requestBypassBatteryOptimizationPermission
import com.ryosoftware.battery_tile.Main.Companion.requestPostNotificationsPermission

class MainActivity : ComponentActivity() {

    private enum class Screen { Main, Selector, TileSettings, NotificationSettings, DebugLog, BatteryInfo, BatteryHistory }

    private var postNotificationsPermissionRequested = false
    private var batteryOptimizationsBypassPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                else -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val prefs = remember { BatteryTilePreferences(context) }
                    val notifPrefs = remember { NotificationPreferences(context) }
                    val appPrefs = remember { AppPreferences(context) }
                    val startInSelector = remember { !appPrefs.isFirstRun }
                    var screen by remember { mutableStateOf<Screen>(if (startInSelector) Screen.Selector else Screen.Main) }

                    when (screen) {
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
                                onTileSettings = { screen = Screen.TileSettings },
                                onNotificationSettings = { screen = Screen.NotificationSettings },
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
                                prefs = prefs,
                                onBack = {
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.NotificationSettings -> {
                            BackHandler { screen = Screen.Selector }

                            NotificationSettingsScreen(
                                prefs = prefs,
                                notifPrefs = notifPrefs,
                                onBack = {
                                    screen = Screen.Selector
                                }
                            )
                        }

                        Screen.BatteryInfo -> {
                            BackHandler { screen = Screen.Selector }

                            BatteryInfoScreen(
                                prefs = prefs,
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
