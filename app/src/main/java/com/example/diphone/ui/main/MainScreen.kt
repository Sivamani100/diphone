package com.example.diphone.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.diphone.feature.calls.CallsScreen
import com.example.diphone.feature.contacts.ContactsScreen
import com.example.diphone.theme.AppColors

@Composable
fun MainScreen(
    showMissedCallsTab: Boolean = false,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlockFilter: () -> Unit,
    onNavigateToManageContacts: () -> Unit,
    onNavigateToNewContact: () -> Unit,
    onContactClick: (Long) -> Unit,
    onUnknownClick: (String) -> Unit,
    onNavigateToRecordings: () -> Unit,
    onNavigateToContactsSearch: () -> Unit,
    onNavigateToCallsSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(if (showMissedCallsTab) 0 else 0) }

    LaunchedEffect(showMissedCallsTab) {
        if (showMissedCallsTab) {
            selectedTab = 0  // Show calls tab when missed call notification is tapped
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = AppColors.Surface,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppColors.Primary,
                        unselectedIconColor = AppColors.Subtitle,
                        selectedTextColor = AppColors.Primary,
                        unselectedTextColor = AppColors.Subtitle,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppColors.Primary,
                        unselectedIconColor = AppColors.Subtitle,
                        selectedTextColor = AppColors.Primary,
                        unselectedTextColor = AppColors.Subtitle,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onNavigateToRecordings() },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Recordings") },
                    label = { Text("Recordings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppColors.Primary,
                        unselectedIconColor = AppColors.Subtitle,
                        selectedTextColor = AppColors.Primary,
                        unselectedTextColor = AppColors.Subtitle,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> CallsScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToBlockFilter = onNavigateToBlockFilter,
                    onNavigateToManageContacts = onNavigateToManageContacts,
                    onContactClick = onContactClick,
                    onUnknownClick = onUnknownClick,
                    onNavigateToSearch = onNavigateToCallsSearch
                )
                1 -> ContactsScreen(
                    onNavigateToDetail = onContactClick,
                    onNavigateToNewContact = onNavigateToNewContact,
                    onNavigateToBlockFilter = onNavigateToBlockFilter,
                    onNavigateToManageContacts = onNavigateToManageContacts,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSearch = onNavigateToContactsSearch
                )
            }
        }
    }
}
