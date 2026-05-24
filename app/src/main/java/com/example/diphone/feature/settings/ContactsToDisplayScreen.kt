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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsToDisplayScreen(onNavigateBack: () -> Unit) {
    var selectedAccount by remember { mutableStateOf("All contacts") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Contacts to display", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                TextButton(onClick = onNavigateBack) {
                    Text("Done", color = AppColors.Primary, fontSize = 16.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        LazyColumn {
            item {
                AccountSelectionItem(
                    title = "All contacts",
                    subtitle = "Total: 1566 contact(s), after smart merge: 593 contact(s)",
                    isSelected = selectedAccount == "All contacts",
                    onClick = { selectedAccount = "All contacts" }
                )
            }
            item {
                AccountSelectionItem(
                    title = "Google\nmallipuramsiva123@gmail.com",
                    subtitle = "Total: 221 contact(s)",
                    isSelected = selectedAccount == "Google1",
                    onClick = { selectedAccount = "Google1" }
                )
            }
            item {
                AccountSelectionItem(
                    title = "WhatsApp",
                    subtitle = "Total: 460 contact(s)",
                    isSelected = selectedAccount == "WhatsApp",
                    onClick = { selectedAccount = "WhatsApp" }
                )
            }
            item {
                AccountSelectionItem(
                    title = "This device",
                    subtitle = "Total: 5 contact(s)",
                    isSelected = selectedAccount == "Device",
                    onClick = { selectedAccount = "Device" }
                )
            }
        }
    }
}

@Composable
fun AccountSelectionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = AppColors.Subtitle, fontSize = 14.sp)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
        )
    }
}
