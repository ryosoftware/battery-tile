package com.ryosoftware.battery_tile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.BatteryTileBatteryIntentHelper.BatteryTileField.Companion.getLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileSettingsScreen(
    prefs: BatteryTilePreferences,
    onBack: () -> Unit
) {
    val fields = remember {
        BatteryTileBatteryIntentHelper.BatteryTileField.entries.sortedWith(compareBy< BatteryTileBatteryIntentHelper.BatteryTileField> { !prefs.isFieldVisible(it) }.thenBy { prefs.getFieldPosition(it) }).toMutableStateList()
    }

    val fieldVisibility = remember {
        mutableStateMapOf<BatteryTileBatteryIntentHelper.BatteryTileField, Boolean>().apply {
            BatteryTileBatteryIntentHelper.BatteryTileField.entries.forEach { put(it, prefs.isFieldVisible(it)) }
        }
    }

    val fieldLine = remember {
        mutableStateMapOf<BatteryTileBatteryIntentHelper.BatteryTileField, Int>().apply {
            BatteryTileBatteryIntentHelper.BatteryTileField.entries.forEach { put(it, prefs.getFieldLine(it)) }
        }
    }

    fun updateOrder() {
        fields.sortWith(compareBy<BatteryTileBatteryIntentHelper.BatteryTileField> { !prefs.isFieldVisible(it) }.thenBy { prefs.getFieldPosition(it) })
    }

    var iconField by remember { mutableStateOf(prefs.iconField) }
    var iconDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tile_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.tile_general),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.tile_icon_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = iconDropdownExpanded,
                onExpandedChange = { iconDropdownExpanded = !iconDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = iconField.getLabel(context),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = iconDropdownExpanded,
                    onDismissRequest = { iconDropdownExpanded = false }
                ) {
                    val iconizableFields = BatteryTileBatteryIntentHelper.BatteryTileField.entries.filter { it.iconizable && it.isSupported }

                    iconizableFields.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.getLabel(context)) },
                            onClick = {
                                iconField = field
                                prefs.iconField = field
                                iconDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.tile_lines_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            val textualizableFields = BatteryTileBatteryIntentHelper.BatteryTileField.entries.filter { it.textualizable && it.isSupported }

            textualizableFields.forEach { field ->
                val visible = fieldVisibility[field] ?: false
                val line = fieldLine[field] ?: 1

                FieldRow(
                    label = field.getLabel(context),
                    checked = visible,
                    line = line,
                    onCheckedChange = { newVisible ->
                        fieldVisibility[field] = newVisible
                        prefs.setFieldVisible(field, newVisible)
                        if (newVisible) {
                            fields.filter { it != field }.forEach {
                                prefs.setFieldPosition(it, prefs.getFieldPosition(it) + 1)
                            }
                            prefs.setFieldPosition(field, 1)
                        } else {
                            val maxPos = fields.maxOf { prefs.getFieldPosition(it) }
                            prefs.setFieldPosition(field, maxPos + 1)
                        }
                        updateOrder()
                    },
                    onLineChange = { newLine ->
                        fieldLine[field] = newLine
                        prefs.setFieldLine(field, newLine)
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldRow(
    label: String,
    checked: Boolean,
    line: Int,
    onCheckedChange: (Boolean) -> Unit,
    onLineChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = null)

            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (checked) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(start = 56.dp, bottom = 16.dp)
            ) {
                SegmentedButton(
                    selected = line == 1,
                    onClick = { onLineChange(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(stringResource(R.string.line_1))
                }
                SegmentedButton(
                    selected = line == 2,
                    onClick = { onLineChange(2) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(stringResource(R.string.line_2))
                }
            }
        }
    }
}
