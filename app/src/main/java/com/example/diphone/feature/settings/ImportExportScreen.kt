package com.example.diphone.feature.settings

import androidx.compose.foundation.background
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
fun ImportExportScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Import/Export contacts", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        LazyColumn {
            item { SettingsHeader("Import contacts") }
            item {
                SettingsArrowItem(
                    title = "Import from storage",
                    onClick = { /* Pick .vcf file */ }
                )
            }
            item {
                SettingsArrowItem(
                    title = "Import from another device",
                    onClick = { /* Bluetooth flow */ }
                )
            }

            item { SettingsHeader("Export contacts") }
            item {
                SettingsArrowItem(
                    title = "Migrate to Google Account",
                    onClick = { /* Migration flow */ }
                )
            }
            item {
                SettingsArrowItem(
                    title = "Export to storage devices",
                    onClick = { /* Export to .vcf */ }
                )
            }

            item { SettingsHeader("Send contacts") }
            item {
                SettingsArrowItem(
                    title = "Send contacts",
                    onClick = { /* Share sheet */ }
                )
            }
        }
    }
}
