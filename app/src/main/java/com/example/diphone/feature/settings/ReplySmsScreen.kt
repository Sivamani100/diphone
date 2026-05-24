package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun ReplySmsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val replySmsEnabled by container.settingsRepository.replySmsEnabled.collectAsState(initial = true)
    val t1 by container.settingsRepository.replyTemplate1.collectAsState(initial = "I'll call you back later.")
    val t2 by container.settingsRepository.replyTemplate2.collectAsState(initial = "Can't talk now. What's up?")
    val t3 by container.settingsRepository.replyTemplate3.collectAsState(initial = "Can't talk now. Call me back later.")
    val t4 by container.settingsRepository.replyTemplate4.collectAsState(initial = "I'll be there soon.")

    val templates = listOf(t1, t2, t3, t4)

    // Dialog state
    var editingIndex by remember { mutableIntStateOf(-1) }
    var editText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Reply with SMS", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { scope.launch { container.settingsRepository.resetReplies() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        LazyColumn {
            item {
                SettingsToggleItem(
                    title = "Reply with SMS",
                    subtitle = "Quickly reply to incoming calls with the messages below. You can tap on them to edit their content.",
                    isChecked = replySmsEnabled,
                    onCheckedChange = { scope.launch { container.settingsRepository.updateReplySmsEnabled(it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
            }

            itemsIndexed(templates) { index, text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = replySmsEnabled) {
                            editingIndex = index
                            editText = text
                        }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = text,
                        color = if (replySmsEnabled) Color.White else AppColors.Subtitle,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < templates.size - 1) {
                    HorizontalDivider(
                        color = AppColors.Divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Edit Dialog
    if (editingIndex >= 0) {
        AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            containerColor = AppColors.Surface,
            title = { Text("Edit message", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Subtitle
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (editingIndex) {
                            0 -> container.settingsRepository.updateReplyTemplate1(editText)
                            1 -> container.settingsRepository.updateReplyTemplate2(editText)
                            2 -> container.settingsRepository.updateReplyTemplate3(editText)
                            3 -> container.settingsRepository.updateReplyTemplate4(editText)
                        }
                    }
                    editingIndex = -1
                }) {
                    Text("Save", color = AppColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = -1 }) {
                    Text("Cancel", color = AppColors.Subtitle)
                }
            }
        )
    }
}
