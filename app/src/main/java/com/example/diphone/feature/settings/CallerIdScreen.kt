package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun CallerIdScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val callerIdEnabled by container.settingsRepository.callerIdEnabled.collectAsState(initial = true)
    val quickActionsEnabled by container.settingsRepository.quickActionsEnabled.collectAsState(initial = true)
    val updateMobileEnabled by container.settingsRepository.updateMobileNetwork.collectAsState(initial = true)
    val experienceProgEnabled by container.settingsRepository.identificationProgramme.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Caller identification", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsToggleItem(
                title = "Caller identification",
                subtitle = "Identify and flag unknown phone numbers online.",
                isChecked = callerIdEnabled,
                onCheckedChange = { scope.launch { container.settingsRepository.updateCallerIdEnabled(it) } }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Quick actions panel",
                subtitle = "After ending a call from an unknown number for the first time, you can choose to save the number, add it to the blocklist or flag it.",
                isChecked = quickActionsEnabled,
                onCheckedChange = { scope.launch { container.settingsRepository.updateQuickActionsEnabled(it) } },
                enabled = callerIdEnabled
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Update over mobile network",
                subtitle = "Update the number database using mobile data.",
                isChecked = updateMobileEnabled,
                onCheckedChange = { scope.launch { container.settingsRepository.updateUpdateMobileNetwork(it) } },
                enabled = callerIdEnabled
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleItem(
                title = "Call Identification Experience Programme",
                subtitle = "Upload identification data to our servers to improve services.",
                isChecked = experienceProgEnabled,
                onCheckedChange = { scope.launch { container.settingsRepository.updateIdentificationProgramme(it) } },
                enabled = callerIdEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsArrowItem(
                title = "About",
                subtitle = "About this feature · User Agreement · Privacy Notice",
                onClick = { /* Navigate to About Caller ID */ }
            )
        }
    }
}
