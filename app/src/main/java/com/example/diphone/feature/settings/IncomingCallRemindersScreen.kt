package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.DiPhoneApp
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallRemindersScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val incomingCallStyle by container.settingsRepository.incomingCallStyle.collectAsState(initial = "Banner")
    val flashOnCall by container.settingsRepository.flashOnCall.collectAsState(initial = false)
    val ascendingRingtone by container.settingsRepository.ascendingRingtone.collectAsState(initial = true)

    val isBannerStyle = incomingCallStyle == "Banner"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Incoming call reminders", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Style Selector Header
            Text(
                "Call display style",
                color = AppColors.Subtitle,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Banner Style Mockup
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        scope.launch { container.settingsRepository.updateIncomingCallStyle("Banner") }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Surface)
                            .border(
                                2.dp,
                                if (isBannerStyle) AppColors.Primary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Banner strip at top of phone mockup
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.SurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.Primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Incoming call", color = Color.White, fontSize = 7.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isBannerStyle,
                            onClick = { scope.launch { container.settingsRepository.updateIncomingCallStyle("Banner") } },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Text("Banner", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Full Screen Style Mockup
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        scope.launch { container.settingsRepository.updateIncomingCallStyle("Full screen") }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.CallBackground)
                            .border(
                                2.dp,
                                if (!isBannerStyle) AppColors.Primary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(AppColors.SurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Incoming", color = Color.White, fontSize = 6.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isBannerStyle,
                            onClick = { scope.launch { container.settingsRepository.updateIncomingCallStyle("Full screen") } },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Text("Full screen", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Applies only to incoming calls received when your device is unlocked.",
                color = AppColors.Subtitle,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)

        SettingsToggleItem(
            title = "Flash on call",
            subtitle = "The camera flash will blink when you receive an incoming call and the screen is locked.",
            isChecked = flashOnCall,
            onCheckedChange = { scope.launch { container.settingsRepository.updateFlashOnCall(it) } }
        )

        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsToggleItem(
            title = "Ascending ringtone",
            subtitle = "Gradually increases ringtone volume from 0 to current volume over the course of ringing.",
            isChecked = ascendingRingtone,
            onCheckedChange = { scope.launch { container.settingsRepository.updateAscendingRingtone(it) } }
        )
    }
}
