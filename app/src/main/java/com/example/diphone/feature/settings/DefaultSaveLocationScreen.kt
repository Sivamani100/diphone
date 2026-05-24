package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSaveLocationScreen(onNavigateBack: () -> Unit) {
    var selectedLocation by remember { mutableStateOf("This device") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("Default save location", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLocation = "This device" }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("This device", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = selectedLocation == "This device",
                        onClick = { selectedLocation = "This device" },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                    )
                }
            }
            item {
                HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            // Mock google accounts
            val accounts = listOf(
                "Google — 241ucs0288@ggu.edu.in",
                "Google — mallipuramsiva123@gmail.com",
                "Google — okayrahaa@gmail.com"
            )
            
            accounts.forEach { account ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLocation = account }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(account, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = selectedLocation == account,
                            onClick = { selectedLocation = account },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                    }
                }
                item {
                    HorizontalDivider(color = AppColors.Divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Add account intent */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = AppColors.Primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add account", color = AppColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
