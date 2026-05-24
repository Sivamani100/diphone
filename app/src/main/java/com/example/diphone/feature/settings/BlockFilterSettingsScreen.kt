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
fun BlockFilterSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBlockCalls: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Block & filter", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        Column {
            SettingsArrowItem(
                title = "Block calls",
                onClick = onNavigateToBlockCalls
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsArrowItem(
                title = "Block messages",
                onClick = { /* navigate */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsArrowItem(
                title = "Blocklist",
                subtitle = "None",
                onClick = { /* navigate */ }
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsArrowItem(
                title = "Allowlist",
                subtitle = "2 items",
                onClick = { /* navigate */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsArrowItem(
                title = "When calls or messages are blocked",
                subtitle = "Notify me",
                onClick = { /* open dropdown */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "The default messaging app is a third-party app. Messages cannot be blocked, but call blocking and the blocklist and allowlist will not be affected.",
                color = AppColors.Subtitle,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
