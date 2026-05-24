package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.DiPhoneApp
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerEndCallsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReplySms: () -> Unit
) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val vibrateOnAnswer by container.settingsRepository.vibrateOnAnswer.collectAsState(initial = true)
    val autoAnswer by container.settingsRepository.autoAnswerEnabled.collectAsState(initial = false)
    val autoAnswerDelay by container.settingsRepository.autoAnswerDelay.collectAsState(initial = 5)
    val powerButtonEnds by container.settingsRepository.powerButtonEndsCall.collectAsState(initial = true)
    val defaultToSpeaker by container.settingsRepository.defaultToSpeaker.collectAsState(initial = false)

    val delayOptions = listOf(1, 3, 5, 10)
    var showDelayDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Answer/End calls", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        Column {
            SettingsToggleItem(
                title = "Vibrate when your calls are answered/ended",
                isChecked = vibrateOnAnswer,
                onCheckedChange = { scope.launch { container.settingsRepository.updateVibrateOnAnswer(it) } }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Auto-answer calls when connected to external audio device",
                subtitle = "Includes Bluetooth and wired headphones",
                isChecked = autoAnswer,
                onCheckedChange = { scope.launch { container.settingsRepository.updateAutoAnswerEnabled(it) } }
            )

            if (autoAnswer) {
                Box {
                    SettingsArrowItem(
                        title = "Delay auto answer",
                        subtitle = "$autoAnswerDelay seconds",
                        onClick = { showDelayDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showDelayDropdown,
                        onDismissRequest = { showDelayDropdown = false }
                    ) {
                        delayOptions.forEach { delay ->
                            DropdownMenuItem(
                                text = { Text("$delay seconds", color = Color.White) },
                                onClick = {
                                    scope.launch { container.settingsRepository.updateAutoAnswerDelay(delay) }
                                    showDelayDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsArrowItem(
                title = "Reply with SMS",
                onClick = onNavigateToReplySms
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Press Power button to end calls",
                subtitle = "Only works when the screen is on",
                isChecked = powerButtonEnds,
                onCheckedChange = { scope.launch { container.settingsRepository.updatePowerButtonEndsCall(it) } }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Calls default to speaker",
                isChecked = defaultToSpeaker,
                onCheckedChange = { scope.launch { container.settingsRepository.updateDefaultToSpeaker(it) } }
            )
        }
    }
}
