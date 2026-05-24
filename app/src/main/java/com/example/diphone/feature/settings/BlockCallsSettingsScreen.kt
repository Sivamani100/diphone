package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockCallsSettingsScreen(onNavigateBack: () -> Unit) {
    var blockAll by remember { mutableStateOf(false) }
    var blockUnknown by remember { mutableStateOf(false) }
    var blockOneRing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Block calls", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        Text(
            "VIP contacts and numbers added to the allowlist will not be blocked.",
            color = AppColors.Primary,
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp)
        )

        Column {
            SettingsToggleItem(
                title = "Block all calls",
                isChecked = blockAll,
                onCheckedChange = { blockAll = it }
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsToggleItem(
                title = "Block calls from unknown numbers",
                isChecked = blockUnknown,
                onCheckedChange = { blockUnknown = it }
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsToggleItem(
                title = "Block one-ring calls",
                isChecked = blockOneRing,
                onCheckedChange = { blockOneRing = it }
            )
        }
    }
}
