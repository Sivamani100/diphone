package com.example.diphone.feature.calls

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.repository.CallLogEntry
import com.example.diphone.core.data.system.PhoneCallHelper
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsSearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Live-filtered stream of all call logs
    val callLogs by remember(searchQuery) {
        container.callLogRepository.getCallLogsStream(onlyMissed = false)
            .map { list ->
                if (searchQuery.isBlank()) emptyList()
                else {
                    val q = searchQuery.lowercase().trim()
                    list.filter {
                        (it.name?.lowercase()?.contains(q) == true) || it.number.contains(q)
                    }
                }
            }
    }.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search call history...", color = AppColors.Subtitle) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AppColors.Primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppColors.Subtitle)
                    }
                }
            }

            HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)

            when {
                searchQuery.isBlank() -> {
                    // Hint state
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = AppColors.SurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Type a name or number to search",
                                color = AppColors.Subtitle,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                callLogs.isEmpty() -> {
                    // No results
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = AppColors.SurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No matching call records", color = AppColors.Subtitle, fontSize = 15.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(callLogs) { index, entry ->
                            CallSearchResultItem(
                                entry = entry,
                                searchQuery = searchQuery,
                                onCallBack = { PhoneCallHelper.placeCall(context, entry.number) },
                                onSendSms = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${entry.number}"))
                                    context.startActivity(intent)
                                },
                                onBlock = {
                                    scope.launch {
                                        container.blockedRepository.blockNumber(entry.number, entry.name)
                                    }
                                }
                            )
                            if (index < callLogs.size - 1) {
                                HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallSearchResultItem(
    entry: CallLogEntry,
    searchQuery: String,
    onCallBack: () -> Unit,
    onSendSms: () -> Unit,
    onBlock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallBack() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call type avatar
        val typeColor = when (entry.type) {
            3 -> AppColors.Warning          // Missed
            4 -> AppColors.Blocked          // Blocked
            else -> Color.White
        }
        val typeIcon = when (entry.type) {
            1 -> Icons.Default.CallReceived
            2 -> Icons.Default.CallMade
            3 -> Icons.Default.CallMissed
            4 -> Icons.Default.Block
            else -> Icons.Default.Call
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(typeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name / number with highlighting
        Column(modifier = Modifier.weight(1f)) {
            val displayText = entry.name ?: entry.number
            val annotated = buildAnnotatedString {
                val lower = displayText.lowercase()
                val qLower = searchQuery.lowercase().trim()
                if (qLower.isNotBlank() && lower.contains(qLower)) {
                    val start = lower.indexOf(qLower)
                    val end = start + qLower.length
                    append(displayText.substring(0, start))
                    withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold)) {
                        append(displayText.substring(start, end))
                    }
                    append(displayText.substring(end))
                } else {
                    append(displayText)
                }
            }
            Text(
                text = annotated,
                color = typeColor.takeIf { entry.type == 3 || entry.type == 4 } ?: Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            val subText = buildString {
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
                append(entry.number)
            }
            Text(subText, color = AppColors.Subtitle, fontSize = 13.sp)
            Text(
                text = formatLogDate(entry.date),
                color = AppColors.SurfaceVariant,
                fontSize = 11.sp
            )
        }

        // Quick actions
        IconButton(onClick = onCallBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onSendSms, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Message, contentDescription = "Message", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}
