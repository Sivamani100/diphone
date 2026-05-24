package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.theme.AppColors
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlockFilter: () -> Unit,
    onNavigateToManageContacts: () -> Unit,
    onNavigateToCallerId: () -> Unit,
    onNavigateToIncomingReminders: () -> Unit,
    onNavigateToSpeedDial: () -> Unit,
    onNavigateToAnswerEnd: () -> Unit,
    onNavigateToCallRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SettingsItem(
                        title = "Caller identification",
                        subtitle = "On",
                        onClick = onNavigateToCallerId
                    )
                }
                item {
                    SettingsItem(
                        title = "Incoming call reminders",
                        subtitle = "Banner",
                        onClick = onNavigateToIncomingReminders
                    )
                }
                item {
                    SettingsItem(
                        title = "Speed dial",
                        onClick = onNavigateToSpeedDial
                    )
                }
                item {
                    SettingsItem(
                        title = "Answer/End calls",
                        onClick = onNavigateToAnswerEnd
                    )
                }

                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    SettingsItem(
                        title = "Call recording",
                        onClick = onNavigateToCallRecording
                    )
                }
                item {
                    SettingsItem(
                        title = "Block & filter",
                        onClick = onNavigateToBlockFilter
                    )
                }

                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    SettingsHeader("System Settings")
                }
                item {
                    SettingsItem(
                        title = "Set as default calling app",
                        subtitle = "Enable call notifications & features",
                        onClick = {
                            try {
                                android.util.Log.d("SettingsScreen", "User clicked 'Set as default calling app'")
                                com.example.diphone.feature.dialer.requestDefaultDialerRole(context)
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Error in default dialer click: ${e.message}", e)
                            }
                        }
                    )
                }
                item {
                    SettingsItem(
                        title = "Enable notification permissions",
                        subtitle = "Allow ongoing call notifications",
                        onClick = {
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }.let { context.startActivity(it) }
                        }
                    )
                }
                item {
                    SettingsItem(
                        title = "Display over other apps",
                        subtitle = "Show call screen while using other apps",
                        onClick = {
                            android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = android.net.Uri.parse("package:" + context.packageName)
                            }.let { context.startActivity(it) }
                        }
                    )
                }

                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    SettingsItem(
                        title = "Operator-related settings",
                        onClick = { /* Handle navigation */ }
                    )
                }
                item {
                    SettingsItem(
                        title = "More settings",
                        onClick = { /* Handle navigation */ }
                    )
                }

                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    SettingsHeader("You might be looking for:")
                }
                item {
                    SettingsItem(
                        title = "Mobile network",
                        onClick = { /* Handle navigation */ }
                    )
                }
                
                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }
                
                item {
                    SettingsItem(
                        title = "Manage contacts",
                        onClick = onNavigateToManageContacts
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = AppColors.Primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 72.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Subtitle,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = AppColors.Subtitle,
                    fontSize = 14.sp
                )
            }
        }
    }
}
