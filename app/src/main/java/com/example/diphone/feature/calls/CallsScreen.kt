package com.example.diphone.feature.calls

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.repository.CallLogEntry
import com.example.diphone.core.data.system.CallManager
import com.example.diphone.core.data.system.PhoneCallHelper
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CallsScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBlockFilter: () -> Unit,
    onNavigateToManageContacts: () -> Unit,
    onContactClick: (Long) -> Unit,
    onUnknownClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    // Screen state
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Missed
    var showMenu by remember { mutableStateOf(false) }

    // Multi-select Delete Mode state
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedEntries = remember { mutableStateListOf<Long>() }

    // Bottom sheets state
    var activeDetailsEntry by remember { mutableStateOf<CallLogEntry?>(null) }
    var showDialPad by remember { mutableStateOf(false) }

    // Load call logs
    val logsFlow = remember(selectedTab) {
        container.callLogRepository.getCallLogsStream(onlyMissed = (selectedTab == 1))
    }
    val callLogs by logsFlow.collectAsState(initial = emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. App Top Bar
            if (isSelectMode) {
                // Multi-select actions top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSelectMode = false
                        selectedEntries.clear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "${selectedEntries.size} Selected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = {
                        if (selectedEntries.size == callLogs.size) {
                            selectedEntries.clear()
                        } else {
                            selectedEntries.clear()
                            selectedEntries.addAll(callLogs.map { it.id })
                        }
                    }) {
                        Text("Select All", color = AppColors.Primary)
                    }
                }
            } else {
                // Standard Title Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All / Missed Pill Segmented Toggle
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
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "All",
                                color = if (selectedTab == 0) Color.White else AppColors.Subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { selectedTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 1) Color.White.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "Missed",
                                color = if (selectedTab == 1) Color.White else AppColors.Subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { onNavigateToSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
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
                                    text = { Text("Edit Logs", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        isSelectMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block & filter", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToBlockFilter()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage contacts", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToManageContacts()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Call Logs List
            if (callLogs.isEmpty()) {
                // Empty view state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = AppColors.SurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No call history logs found",
                            color = AppColors.Subtitle,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(callLogs, key = { it.id }) { entry ->
                        val isSelected = selectedEntries.contains(entry.id)
                        
                        CallLogItem(
                            entry = entry,
                            isSelected = isSelected,
                            isSelectMode = isSelectMode,
                            onClick = {
                                if (isSelectMode) {
                                    if (isSelected) selectedEntries.remove(entry.id)
                                    else selectedEntries.add(entry.id)
                                } else {
                                    // Directly call — tap on any recent call entry always dials
                                    PhoneCallHelper.placeCall(context, entry.number)
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) {
                                    isSelectMode = true
                                    selectedEntries.add(entry.id)
                                }
                            },
                            onInfoClick = {
                                activeDetailsEntry = entry
                            }
                        )
                        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                    }
                }
            }
        }

        // 3. Multi-Select Bottom Deletion Panel
        if (isSelectMode && selectedEntries.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(AppColors.Surface)
                    .clickable {
                        // Confirm deletion dialog
                        scope.launch {
                            selectedEntries.forEach { id ->
                                container.callLogRepository.deleteCallLogEntry(id)
                            }
                            selectedEntries.clear()
                            isSelectMode = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Decline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete (${selectedEntries.size})",
                        color = AppColors.Decline,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 4. Dial Pad Slide-up FAB
        if (!isSelectMode) {
            FloatingActionButton(
                onClick = { showDialPad = true },
                containerColor = AppColors.Primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(64.dp)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = "Dial Pad", tint = Color.White)
            }
        }

        // 5. Call Log Entry Details Info Bottom Sheet
        if (activeDetailsEntry != null) {
            val entry = activeDetailsEntry!!
            ModalBottomSheet(
                onDismissRequest = { activeDetailsEntry = null },
                containerColor = AppColors.Surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = entry.name ?: "Unknown Number",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = entry.number,
                        fontSize = 16.sp,
                        color = AppColors.Subtitle,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Call Type", color = AppColors.Subtitle)
                        Text(
                            text = when(entry.type) {
                                1 -> "Incoming Call"
                                2 -> "Outgoing Call"
                                3 -> "Missed Call"
                                4 -> "Blocked Call"
                                else -> "Call"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duration", color = AppColors.Subtitle)
                        Text(
                            text = if (entry.duration > 0) "${entry.duration / 60}m ${entry.duration % 60}s" else "0s",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date & Time", color = AppColors.Subtitle)
                        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        Text(
                            text = formatter.format(Date(entry.date)),
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Quick actions
                        IconButton(onClick = {
                            activeDetailsEntry = null
                            PhoneCallHelper.placeCall(context, entry.number)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call Back", tint = AppColors.Primary)
                        }
                        IconButton(onClick = {
                            activeDetailsEntry = null
                            val smsUri = Uri.parse("smsto:${entry.number}")
                            val intent = Intent(Intent.ACTION_SENDTO, smsUri)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Message, contentDescription = "Send Message", tint = Color.White)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                container.blockedRepository.blockNumber(entry.number, entry.name)
                                activeDetailsEntry = null
                            }
                        }) {
                            Icon(Icons.Default.Block, contentDescription = "Block", tint = AppColors.Warning)
                        }
                    }
                }
            }
        }

        // 6. Dial Pad slide-up panel sheet
        if (showDialPad) {
            ModalBottomSheet(
                onDismissRequest = { showDialPad = false },
                containerColor = AppColors.Surface
            ) {
                DialPadSheet(
                    onCall = { number ->
                        showDialPad = false
                        PhoneCallHelper.placeCall(context, number)
                    }
                )
            }
        }
    }
}

@Composable
fun CallLogItem(
    entry: CallLogEntry,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        // 1. Call Direction Arrow Icon
        val arrowIcon = when(entry.type) {
            1 -> Icons.Default.CallReceived     // Incoming (white)
            2 -> Icons.Default.CallMade         // Outgoing (white)
            3 -> Icons.Default.CallReceived     // Missed (warning orange)
            4 -> Icons.Default.Block            // Blocked (orange)
            else -> Icons.Default.Call
        }
        val tintColor = when(entry.type) {
            3 -> AppColors.Warning
            4 -> AppColors.Blocked
            else -> Color.White
        }

        Icon(
            imageVector = arrowIcon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // 2. Caller Details column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name ?: entry.number,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.type == 3) AppColors.Warning else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtitle Details (badges and phone locations)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // HD and VoLTE Badges
                if (entry.hasVoLTE) {
                    BadgePill(text = "Vo/LTE")
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (entry.hasHD) {
                    BadgePill(text = "HD")
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (entry.isRecorded) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recorded",
                        tint = AppColors.Subtitle,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                // Location / Number Label & Duration
                val subText = buildString {
                    if (entry.spamTag != null) {
                        append(entry.spamTag)
                    } else {
                        val typeText = when (entry.type) {
                            1 -> "Incoming"
                            2 -> "Outgoing"
                            3 -> "Missed"
                            4 -> "Blocked"
                            else -> "Call"
                        }
                        append(typeText)
                        if (entry.type != 3 && entry.type != 4 && entry.duration > 0) {
                            val mins = entry.duration / 60
                            val secs = entry.duration % 60
                            append(" (")
                            if (mins > 0) append("${mins}m ")
                            append("${secs}s)")
                        }
                        append(" • ")
                        append(entry.name?.let { entry.number } ?: "Mobile | India")
                    }
                }
                
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = if (entry.spamTag != null) AppColors.Warning else AppColors.Subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Right actions (Time and Info Button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Formatted Date or Time
            val formattedDate = formatLogDate(entry.date)
            Text(
                text = formattedDate,
                fontSize = 13.sp,
                color = AppColors.Subtitle
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary)
                )
            } else {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = AppColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray
        )
    }
}

fun formatLogDate(timestamp: Long): String {
    val smsCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCalendar = Calendar.getInstance()
    
    return when {
        smsCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                smsCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR) -> {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.format(Date(timestamp))
        }
        smsCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                smsCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR) - 1 -> {
            "Yesterday"
        }
        else -> {
            val format = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            format.format(Date(timestamp))
        }
    }
}
