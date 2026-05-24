package com.example.diphone.feature.contacts

import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.repository.Contact
import com.example.diphone.core.data.system.SoundHelper
import com.example.diphone.core.data.system.PhoneCallHelper
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val contact by container.contactsRepository.getContactByIdStream(contactId).collectAsState(initial = null)
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(contactId) {
        delay(150)
        isLoading = false
    }

    // Load and filter call logs reactively
    val callLogs by container.callLogRepository.getCallLogsStream().collectAsState(initial = emptyList())
    val contactCallLogs = remember(contact, callLogs) {
        val c = contact
        if (c == null) emptyList()
        else {
            // Normalize contact numbers by extracting only digits
            val normalizedContactNumbers = c.phoneNumbers.map { num ->
                num.filter { it.isDigit() }
                    .removePrefix("1") // Remove leading 1 if it's a US number
                    .takeLast(10) // Keep last 10 digits for matching
            }.filter { it.isNotEmpty() }
            
            // Filter call logs that match this contact
            callLogs.filter { log ->
                val normalizedLogNum = log.number.filter { it.isDigit() }
                    .removePrefix("1")
                    .takeLast(10)
                
                normalizedLogNum.isNotEmpty() && 
                normalizedContactNumbers.any { contactNum ->
                    // Exact match after normalization
                    normalizedLogNum == contactNum ||
                    // Also match if one is a substring (for partial numbers)
                    (normalizedLogNum.length >= 7 && normalizedLogNum.endsWith(contactNum.takeLast(7)))
                }
            }
        }
    }

    // Query system ringtones for picker list
    val ringtones = remember {
        try {
            val list = mutableListOf(Pair("Default", "default"))
            val rm = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_RINGTONE) }
            val cursor = rm.cursor
            var count = 0
            while (cursor.moveToNext() && count < 6) {
                val title = cursor.getString(1)
                val uri = rm.getRingtoneUri(cursor.position)?.toString()
                if (title != null && uri != null) {
                    list.add(Pair(title, uri))
                }
                count++
            }
            list
        } catch (e: Exception) {
            listOf(
                Pair("Default", "default"),
                Pair("Classic Bell", "content://media/internal/audio/media/1"),
                Pair("Vibrant Digital", "content://media/internal/audio/media/2")
            )
        }
    }

    var showRingtoneDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppColors.Primary)
        } else if (contact == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Contact not found", color = Color.White, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) {
                    Text("Go Back")
                }
            }
        } else {
            val c = contact!!
            
            // Ringtone picker dialog
            if (showRingtoneDialog) {
                AlertDialog(
                    onDismissRequest = { showRingtoneDialog = false },
                    title = { Text("Select Ringtone", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    containerColor = AppColors.Surface,
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(ringtones) { ringtone ->
                                val isSelected = (ringtone.second == "default" && c.customRingtone == null) ||
                                        (ringtone.second == c.customRingtone)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val saveUri = if (ringtone.second == "default") null else ringtone.second
                                                container.contactsRepository.updateCustomRingtone(c.id, saveUri)
                                                showRingtoneDialog = false
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch {
                                                val saveUri = if (ringtone.second == "default") null else ringtone.second
                                                container.contactsRepository.updateCustomRingtone(c.id, saveUri)
                                                showRingtoneDialog = false
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = ringtone.first, color = Color.White, fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRingtoneDialog = false }) {
                            Text("Cancel", color = AppColors.Primary)
                        }
                    }
                )
            }

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
                    Row {
                        IconButton(onClick = {
                            scope.launch {
                                container.contactsRepository.toggleVipStatus(c.id, !c.isVip)
                            }
                        }) {
                            Icon(
                                if (c.isVip) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "VIP",
                                tint = if (c.isVip) AppColors.VipStar else Color.White
                            )
                        }
                        IconButton(onClick = { onEdit(c.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    // Avatar & Name Header (Asynchronously Loaded to prevent frame-drops)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (c.isVip) AppColors.VipStar.copy(alpha = 0.15f)
                                        else AppColors.Surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (c.photoUri != null) {
                                    var bitmap by remember(c.photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    LaunchedEffect(c.photoUri) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val uri = android.net.Uri.parse(c.photoUri)
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap!!.asImageBitmap(),
                                            contentDescription = c.name,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = c.initial.toString(),
                                            color = if (c.isVip) AppColors.VipStar else AppColors.Primary,
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = c.initial.toString(),
                                        color = if (c.isVip) AppColors.VipStar else AppColors.Primary,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = c.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (c.company.isNotBlank() || c.jobTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = listOf(c.jobTitle, c.company).filter { it.isNotBlank() }.joinToString(" at "),
                                    fontSize = 14.sp,
                                    color = AppColors.Subtitle
                                )
                            }
                        }
                    }

                    // Quick Actions (Call, Text, Video)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val primaryNumber = c.phoneNumbers.firstOrNull()
                            
                            QuickActionItem(
                                icon = Icons.Default.Call,
                                label = "Call",
                                enabled = primaryNumber != null,
                                onClick = { primaryNumber?.let { PhoneCallHelper.placeCall(context, it) } }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Message,
                                label = "Text",
                                enabled = primaryNumber != null,
                                onClick = {
                                    primaryNumber?.let {
                                        val smsUri = Uri.parse("smsto:$it")
                                        val intent = Intent(Intent.ACTION_SENDTO, smsUri)
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Videocam,
                                label = "Video",
                                enabled = primaryNumber != null,
                                onClick = {
                                    primaryNumber?.let {
                                        // Standard video calling intent
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:$it"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }

                    // Contact Info Section
                    item {
                        SectionCard(title = "Contact info") {
                            c.phoneNumbers.forEachIndexed { index, number ->
                                DetailRow(
                                    title = number,
                                    subtitle = "Mobile",
                                    icon = Icons.Default.Call,
                                    onClick = { PhoneCallHelper.placeCall(context, number) }
                                )
                                if (index < c.phoneNumbers.size - 1) {
                                    HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                            if (c.emails.isNotEmpty()) {
                                HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 56.dp))
                                c.emails.forEachIndexed { index, email ->
                                    DetailRow(
                                        title = email,
                                        subtitle = "Email",
                                        icon = Icons.Default.Email,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                            context.startActivity(intent)
                                        }
                                    )
                                    if (index < c.emails.size - 1) {
                                        HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 56.dp))
                                    }
                                }
                            }
                        }
                    }

                    // WhatsApp Integration
                    item {
                        SectionCard(title = "WhatsApp") {
                            DetailRow(
                                title = "Message ${c.name}",
                                subtitle = "WhatsApp",
                                icon = Icons.Default.ChatBubbleOutline,
                                onClick = {
                                    val primaryNumber = c.phoneNumbers.firstOrNull() ?: return@DetailRow
                                    val url = "https://api.whatsapp.com/send?phone=${primaryNumber.replace(Regex("[^0-9+]"), "")}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 56.dp))
                            DetailRow(
                                title = "Voice call ${c.name}",
                                subtitle = "WhatsApp",
                                icon = Icons.Default.Call,
                                onClick = {
                                    val primaryNumber = c.phoneNumbers.firstOrNull() ?: return@DetailRow
                                    val url = "https://api.whatsapp.com/send?phone=${primaryNumber.replace(Regex("[^0-9+]"), "")}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 56.dp))
                            DetailRow(
                                title = "Video call ${c.name}",
                                subtitle = "WhatsApp",
                                icon = Icons.Default.Videocam,
                                onClick = {
                                    val primaryNumber = c.phoneNumbers.firstOrNull() ?: return@DetailRow
                                    val url = "https://api.whatsapp.com/send?phone=${primaryNumber.replace(Regex("[^0-9+]"), "")}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    // Recent Calls Card (Shows Call Logs for this specific contact)
                    if (contactCallLogs.isNotEmpty()) {
                        item {
                            SectionCard(title = "All calls (${contactCallLogs.size})") {
                                contactCallLogs.forEachIndexed { index, log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (icon, color) = when (log.type) {
                                            1 -> Pair(Icons.Default.CallReceived, Color(0xFF4CAF50)) // Incoming
                                            2 -> Pair(Icons.Default.CallMade, AppColors.Primary) // Outgoing
                                            3 -> Pair(Icons.Default.CallMissed, AppColors.Decline) // Missed
                                            4 -> Pair(Icons.Default.Block, Color.Gray) // Blocked
                                            else -> Pair(Icons.Default.CallReceived, Color(0xFF4CAF50))
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val typeText = when (log.type) {
                                                1 -> "Incoming"
                                                2 -> "Outgoing"
                                                3 -> "Missed call"
                                                4 -> "Blocked call"
                                                else -> "Incoming"
                                            }
                                            Text(
                                                text = typeText,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = android.text.format.DateUtils.getRelativeTimeSpanString(
                                                    log.date,
                                                    System.currentTimeMillis(),
                                                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                                                ).toString(),
                                                color = AppColors.Subtitle,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (log.type != 3 && log.type != 4) {
                                            val durationText = buildString {
                                                val h = log.duration / 3600
                                                val m = (log.duration % 3600) / 60
                                                val s = log.duration % 60
                                                if (h > 0) append("${h}h ")
                                                if (m > 0) append("${m}m ")
                                                if (s > 0 || isEmpty()) append("${s}s")
                                            }.trim()
                                            Text(
                                                text = durationText,
                                                color = AppColors.Subtitle,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Text(
                                                text = if (log.type == 4) "Blocked" else "Missed",
                                                color = if (log.type == 4) Color.Gray else AppColors.Decline,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (log.isRecorded) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Recorded",
                                                tint = AppColors.Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (index < contactCallLogs.size - 1) {
                                        HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 50.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Settings Section
                    item {
                        SectionCard(title = "Settings") {
                            val currentRingtoneName = ringtones.find { it.second == c.customRingtone }?.first 
                                ?: ringtones.find { it.second == "default" }?.first 
                                ?: "Default"
                            DetailRow(
                                title = "Ringtone",
                                subtitle = currentRingtoneName,
                                onClick = { showRingtoneDialog = true }
                            )
                            HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(start = 16.dp))
                            DetailRow(
                                title = "Route to voicemail",
                                subtitle = if (c.routeToVoicemail) "On" else "Off",
                                onClick = {
                                    scope.launch {
                                        container.contactsRepository.toggleVoicemailRouting(c.id, !c.routeToVoicemail)
                                    }
                                }
                            )
                        }
                    }

                    // Block Section
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Surface)
                                .clickable {
                                    scope.launch {
                                        container.contactsRepository.toggleBlockedStatus(c.id, !c.isBlocked)
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (c.isBlocked) "Unblock numbers" else "Block numbers",
                                color = AppColors.Decline,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (enabled) AppColors.Surface else AppColors.Surface.copy(alpha = 0.5f))
                .clickable(enabled = enabled, onClick = {
                    SoundHelper.playButtonClickTone()
                    onClick()
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) AppColors.Primary else AppColors.Subtitle,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = if (enabled) Color.White else AppColors.Subtitle, fontSize = 13.sp)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = AppColors.Subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun DetailRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                SoundHelper.playButtonClickTone()
                onClick() 
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = AppColors.Subtitle, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = subtitle, color = AppColors.Subtitle, fontSize = 13.sp)
        }
    }
}
