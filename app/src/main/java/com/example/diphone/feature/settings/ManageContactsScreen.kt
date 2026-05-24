package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ManageContactsScreen(
    onBack: () -> Unit,
    onNavigateToContactsToDisplay: () -> Unit,
    onNavigateToDefaultSaveLocation: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToSimContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val showProfilePicture by container.settingsRepository.showProfilePic.collectAsState(initial = true)
    val showOnlyWithNumbers by container.settingsRepository.onlyContactsWithNumbers.collectAsState(initial = false)
    val sortBy by container.settingsRepository.sortBy.collectAsState(initial = "First name")
    val nameOrder by container.settingsRepository.nameOrder.collectAsState(initial = "First name, last name")

    var sortOrderExpanded by remember { mutableStateOf(false) }
    var nameOrderExpanded by remember { mutableStateOf(false) }
    var mergeExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = "Manage contacts",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SettingsToggleItem(
                        title = "Show contact's profile picture",
                        isChecked = showProfilePicture,
                        onCheckedChange = { scope.launch { container.settingsRepository.updateShowProfilePic(it) } }
                    )
                }
                item {
                    SettingsToggleItem(
                        title = "Only show contacts with numbers",
                        isChecked = showOnlyWithNumbers,
                        onCheckedChange = { scope.launch { container.settingsRepository.updateOnlyContactsWithNumbers(it) } }
                    )
                }

                item { HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    SettingsArrowItem(
                        title = "Contacts to display",
                        onClick = onNavigateToContactsToDisplay
                    )
                }

                item {
                    // Sort order dropdown
                    Box {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrderExpanded = true }
                            .padding(16.dp)
                        ) {
                            Text("Sort contacts by", color = Color.White, fontSize = 16.sp)
                            Text(sortBy, color = AppColors.Subtitle, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        DropdownMenu(
                            expanded = sortOrderExpanded,
                            onDismissRequest = { sortOrderExpanded = false }
                        ) {
                            listOf("First name", "Last name").forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (sortBy == option) {
                                                Text("✓ ", color = AppColors.Primary, fontSize = 14.sp)
                                            }
                                            Text(option, color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        scope.launch { container.settingsRepository.updateSortBy(option) }
                                        sortOrderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Name order dropdown
                    Box {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nameOrderExpanded = true }
                            .padding(16.dp)
                        ) {
                            Text("Name order", color = Color.White, fontSize = 16.sp)
                            Text(nameOrder, color = AppColors.Subtitle, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        DropdownMenu(
                            expanded = nameOrderExpanded,
                            onDismissRequest = { nameOrderExpanded = false }
                        ) {
                            listOf("First name, last name", "Last name, first name").forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (nameOrder == option) {
                                                Text("✓ ", color = AppColors.Primary, fontSize = 14.sp)
                                            }
                                            Text(option, color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        scope.launch { container.settingsRepository.updateNameOrder(option) }
                                        nameOrderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp)) }

                item { SettingsArrowItem(title = "Default save location for contacts", onClick = onNavigateToDefaultSaveLocation) }
                item { SettingsArrowItem(title = "Import/Export contacts", onClick = onNavigateToImportExport) }
                item { SettingsArrowItem(title = "SIM card contacts", onClick = onNavigateToSimContacts) }

                item { HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mergeExpanded = !mergeExpanded }
                        .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Merge duplicate contacts", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text(if (mergeExpanded) "▲" else "▼", color = AppColors.Subtitle, fontSize = 12.sp)
                        }
                        if (mergeExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Smart merge will combine contacts with the same name and overlapping phone numbers.",
                                color = AppColors.Subtitle,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { /* trigger merge algorithm */ },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                            ) {
                                Text("Merge now", color = Color.White)
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    SettingsArrowItem(
                        title = "About Contacts",
                        onClick = { /* Show about */ }
                    )
                }
            }
        }
    }
}
