package com.ryosoftware.battery_tile

import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ryosoftware.battery_tile.ui.components.DynamicBatteryBackground
import com.ryosoftware.battery_tile.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class BatteryInfoActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActivityTheme {
                val context = LocalContext.current
                val batteryManager = remember { context.getSystemService(BATTERY_SERVICE) as BatteryManager }

                val prefs = remember { BatteryTilePreferences(context) }
                val appPrefs = remember { AppPreferences(context) }

                var bgBatteryHelper by remember { mutableStateOf(
                    Main.from(context).batteryIntentProvider.get()?.let { BatteryIntentHelper(context, it, batteryManager) }
                ) }

                LaunchedEffect(Unit) {
                    while (true) {
                        bgBatteryHelper = Main.from(context).batteryIntentProvider.get()?.let { BatteryIntentHelper(context, it, batteryManager) }
                        delay(5000.milliseconds)
                    }
                }

                val sheetState = rememberModalBottomSheetState(true)

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DynamicBatteryBackground(
                            batteryLevel = bgBatteryHelper?.level ?: 50,
                            isCharging = bgBatteryHelper?.isCharging ?: false,
                            temperatureCelsius = bgBatteryHelper?.temperatureCelsius ?: 25f,
                        )

                        BatteryInfoContent(
                            modifier = Modifier.padding(horizontal = Spacing.xxl),
                            prefs = prefs,
                            appPrefs = appPrefs
                        )
                    }
                }
            }
        }
    }
}
