package com.example.diphone.feature.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.local.BlockedNumber
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockFilterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Calls, 1 = Messages
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newBlockNumber by remember { mutableStateOf("") }

    val blockedNumbers by container.blockedRepository.blockedNumbers.collectAsState(initial = emptyList())

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Block & filter",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(AppColors.Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear all", color = AppColors.Decline) },
                            onClick = {
                                showMenu = false
                                scope.launch { container.blockedRepository.clearAll() }
                            }
                        )
                    }
                }
            }

            // Pill Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.Surface)
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color.White.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Calls",
                            color = if (selectedTab == 0) Color.White else AppColors.Subtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) Color.White.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Messages",
                            color = if (selectedTab == 1) Color.White else AppColors.Subtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List
            if (blockedNumbers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = AppColors.SurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No blocked numbers",
                            color = AppColors.Subtitle,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(blockedNumbers, key = { it.number }) { blockedInfo ->
                        BlockedItemRow(
                            blockedInfo = blockedInfo,
                            onUnblock = {
                                scope.launch {
                                    container.blockedRepository.unblockNumber(blockedInfo.number)
                                }
                            }
                        )
                        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                    }
                }
            }
        }

        // Add FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = AppColors.Primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
                .clip(CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Number", tint = Color.White)
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    newBlockNumber = ""
                },
                containerColor = AppColors.Surface,
                title = { Text("Block Number", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newBlockNumber,
                        onValueChange = { newBlockNumber = it },
                        label = { Text("Phone number", color = AppColors.Subtitle) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBlockNumber.isNotBlank()) {
                            scope.launch {
                                container.blockedRepository.blockNumber(newBlockNumber, "Manual entry")
                                showAddDialog = false
                                newBlockNumber = ""
                            }
                        }
                    }) {
                        Text("Block", color = AppColors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        newBlockNumber = ""
                    }) {
                        Text("Cancel", color = AppColors.Subtitle)
                    }
                }
            )
        }
    }
}

@Composable
fun BlockedItemRow(blockedInfo: BlockedNumber, onUnblock: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = AppColors.Decline, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = blockedInfo.contactName ?: blockedInfo.number,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            val subText = if (blockedInfo.contactName != null) blockedInfo.number else blockedInfo.reason
            Text(
                text = subText,
                color = AppColors.Subtitle,
                fontSize = 13.sp
            )
        }
        
        TextButton(onClick = onUnblock) {
            Text("Unblock", color = AppColors.Primary)
        }
    }
}
