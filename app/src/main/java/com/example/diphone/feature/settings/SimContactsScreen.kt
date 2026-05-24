package com.example.diphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
fun SimContactsScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopAppBar(
            title = { Text("SIM1", color = Color.White, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { /* Add SIM Contact */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
                IconButton(onClick = { /* Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        LazyColumn {
            item {
                Text(
                    "A",
                    color = AppColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            
            val mockContacts = listOf("AL *dial", "AL cricket", "AL friendzchat", "AL hello tunes", "ambulance")
            
            mockContacts.forEach { name ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* options */ }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(name, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
