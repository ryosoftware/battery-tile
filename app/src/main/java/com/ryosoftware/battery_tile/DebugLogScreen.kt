package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ryosoftware.battery_tile.Main.Companion.logAppVersion
import java.io.File
import kotlinx.coroutines.launch
@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appPrefs = remember { AppPreferences(context) }
    var loggingEnabled by remember { mutableStateOf(appPrefs.isLoggingToFile) }
    var loggingOnlyWhenCharging by remember { mutableStateOf(appPrefs.isLoggingOnlyWhileCharging) }
    var logContents: String? by remember { mutableStateOf("") }
    var showStartLoggingDialog by remember { mutableStateOf(false) }
    var showStopLoggingDialog by remember { mutableStateOf(false) }
    var canShare by remember { mutableStateOf(false) }
    val app = Main.from(context)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val logFileTime by app.logger.logFileTime.collectAsState()

    LaunchedEffect(logFileTime, loggingEnabled) {
        if (loggingEnabled) {
            val isAtBottom = scrollState.value >= scrollState.maxValue
            val contents = app.logger.getLogFileContents(null)
            logContents = contents?.joinToString("\n")
            canShare = !contents.isNullOrEmpty()
            if (isAtBottom) {
                withFrameNanos { }
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_to_file)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (canShare) {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val shareContent = app.logger.getLogFileContents("es")
                                    if (shareContent == null) {
                                        Toast.makeText(context, R.string.cant_read_log_file, Toast.LENGTH_LONG).show()
                                        return@launch
                                    }

                                    val tempFile = File(context.cacheDir, "debug_log.txt")
                                    tempFile.writeText(shareContent.joinToString("\n"))

                                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.file_provider", tempFile)

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                    showStopLoggingDialog = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.cant_read_log_file, Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_log)
                            )
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        loggingEnabled = !loggingEnabled
                        appPrefs.isLoggingToFile = loggingEnabled

                        val logFile = app.logger.getLogFile()
                        if (loggingEnabled && logFile.length() > 0) {
                            showStartLoggingDialog = true
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.log_to_file),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(4.dp))

                    val linesCount = logContents?.lines()?.size ?: 0

                    Text(
                        text = context.resources.getQuantityString(R.plurals.log_file_lines_count, linesCount, linesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = loggingEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        loggingOnlyWhenCharging = !loggingOnlyWhenCharging
                        appPrefs.isLoggingOnlyWhileCharging = loggingOnlyWhenCharging
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.log_only_while_charging),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Switch(
                    checked = loggingOnlyWhenCharging,
                    onCheckedChange = null,
                    enabled = loggingEnabled
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = logContents ?: stringResource(R.string.no_log_data),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(8.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    val closeStartLoggingDialog = {
        context.logAppVersion()

        showStartLoggingDialog = false
    }

    if (showStartLoggingDialog) {
        AlertDialog(
            onDismissRequest = closeStartLoggingDialog,
            title = { Text(stringResource(R.string.logging_dialog_title)) },
            text = { Text(stringResource(R.string.start_logging_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    app.logger.getLogFile().delete()

                    closeStartLoggingDialog()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = closeStartLoggingDialog) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (showStopLoggingDialog) {
        AlertDialog(
            onDismissRequest = { showStopLoggingDialog = false },
            title = { Text(stringResource(R.string.logging_dialog_title)) },
            text = { Text(stringResource(R.string.stop_logging_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val file = app.logger.getLogFile()
                    file.delete()
                    loggingEnabled = false
                    appPrefs.isLoggingToFile = false
                    showStopLoggingDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopLoggingDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
