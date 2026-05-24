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
fun CallRecordingScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val recordAllCalls by container.settingsRepository.recordAllCalls.collectAsState(initial = false)
    val maxRecordings by container.settingsRepository.maxRecordings.collectAsState(initial = "No limit")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Call recording", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        SettingsSectionHeader("Auto recording")

        Column {
            SettingsToggleItem(
                title = "Record all calls",
                subtitle = "Recording begins when the call connects",
                isChecked = recordAllCalls,
                onCheckedChange = { scope.launch { container.settingsRepository.updateRecordAllCalls(it) } }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsArrowItem(
                title = "Maximum recordings",
                subtitle = maxRecordings,
                onClick = { /* Open limit dropdown */ }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsArrowItem(
                title = "View call recordings",
                subtitle = "Open recordings folder",
                onClick = { /* Open file browser at /Recordings/CallRecordings/ */ }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Comply with local laws and regulations and obtain the other party's consent when using call recording.",
                color = AppColors.Subtitle,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
