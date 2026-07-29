package com.ryosoftware.battery_tile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryosoftware.battery_tile.ui.components.GlassGradientBackground
import com.ryosoftware.battery_tile.ui.components.GradientButton

@Composable
fun MainScreen(onSettings: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassGradientBackground(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.surface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(
                                animationSpec = tween(600),
                                initialOffsetY = { -it / 4 },
                            ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.1f),
                                    ),
                                ),
                                shape = MaterialTheme.shapes.large,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 150)) +
                            slideInVertically(
                                animationSpec = tween(600, delayMillis = 150),
                                initialOffsetY = { it / 4 },
                            ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.general_information),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)),
            ) {
                GradientButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
