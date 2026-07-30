package com.ryosoftware.battery_tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ryosoftware.battery_tile.ui.theme.Spacing
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class BatteryInfoActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActivityTheme {
                val context = LocalContext.current

                val prefs = remember { BatteryTilePreferences(context) }
                val appPrefs = remember { AppPreferences(context) }

                val sheetState = rememberModalBottomSheetState(true)

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState
                ) {
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
