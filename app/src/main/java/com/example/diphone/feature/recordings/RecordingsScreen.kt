package com.example.diphone.feature.recordings

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.local.CallRecording
import com.example.diphone.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { com.example.diphone.DiPhoneApp.getContainer(context) }
    val scope = rememberCoroutineScope()

    val recordings by container.appDao.getAllCallRecordings().collectAsState(initial = emptyList())

    // Sync: scan filesystem for any recordings not tracked in the DB
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val dirs = listOfNotNull(
                context.getExternalFilesDir("Recordings"),
                File(context.filesDir, "Recordings")
            )
            dirs.forEach { dir ->
                if (!dir.exists()) return@forEach
                dir.listFiles()?.filter { it.extension == "m4a" || it.extension == "mp4" }?.forEach { file ->
                    val existingInDb = recordings.any { it.filePath == file.absolutePath }
                    if (!existingInDb) {
                        // Try to read duration via MediaMetadataRetriever
                        var durationMs = 0L
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            durationMs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L)
                            retriever.release()
                        } catch (_: Exception) {}
                        // Parse name/number from filename: Name_number_timestamp.m4a
                        val parts = file.nameWithoutExtension.split("_")
                        val contactName = if (parts.size >= 2) parts[0].replace("_", " ") else null
                        val phoneNumber = if (parts.size >= 2) parts[1] else file.nameWithoutExtension
                        container.appDao.insertCallRecording(
                            CallRecording(
                                contactName = contactName,
                                phoneNumber = phoneNumber,
                                filePath = file.absolutePath,
                                duration = durationMs,
                                fileSize = file.length(),
                                recordedAt = file.lastModified(),
                                callType = 1
                            )
                        )
                    }
                }
            }
        }
    }

    // Player state
    var playingId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playerProgress by remember { mutableFloatStateOf(0f) }
    var playerDuration by remember { mutableIntStateOf(0) }
    var playerPosition by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    // Delete confirmation
    var deletingRecording by remember { mutableStateOf<CallRecording?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    // Progress updater
    LaunchedEffect(playingId, isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            try {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    playerPosition = mp.currentPosition
                    playerDuration = mp.duration.coerceAtLeast(1)
                    playerProgress = playerPosition.toFloat() / playerDuration.toFloat()
                }
            } catch (_: Exception) {}
            delay(200)
        }
    }

    fun startPlayback(recording: CallRecording) {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}

        val file = File(recording.filePath)
        if (!file.exists()) return

        val mp = MediaPlayer()
        mp.setDataSource(recording.filePath)
        mp.prepare()
        mp.start()
        mp.setOnCompletionListener {
            isPlaying = false
            playerProgress = 0f
            playerPosition = 0
            playingId = null
        }
        mediaPlayer = mp
        playingId = recording.id
        playerDuration = mp.duration
        playerPosition = 0
        playerProgress = 0f
        isPlaying = true
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        playingId = null
        isPlaying = false
        playerProgress = 0f
        playerPosition = 0
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            mp.start()
            isPlaying = true
        }
    }

    // Delete confirmation dialog
    if (deletingRecording != null) {
        AlertDialog(
            onDismissRequest = { deletingRecording = null },
            containerColor = AppColors.Surface,
            title = { Text("Delete Recording", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to delete this recording? This cannot be undone.",
                    color = AppColors.Subtitle
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val rec = deletingRecording!!
                    scope.launch {
                        // Stop if currently playing this
                        if (playingId == rec.id) stopPlayback()
                        // Delete file
                        try { File(rec.filePath).delete() } catch (_: Exception) {}
                        container.appDao.deleteCallRecording(rec)
                    }
                    deletingRecording = null
                }) {
                    Text("Delete", color = AppColors.Decline)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecording = null }) {
                    Text("Cancel", color = AppColors.Subtitle)
                }
            }
        )
    }

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
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Call Recordings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (recordings.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = null,
                            tint = AppColors.SurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No recordings yet",
                            color = AppColors.Subtitle,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the record button during a call\nto save a recording here",
                            color = AppColors.SurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recordings, key = { it.id }) { recording ->
                        val isCurrentPlaying = playingId == recording.id
                        RecordingItem(
                            recording = recording,
                            isPlaying = isCurrentPlaying && isPlaying,
                            isExpanded = isCurrentPlaying,
                            progress = if (isCurrentPlaying) playerProgress else 0f,
                            currentPos = if (isCurrentPlaying) playerPosition else 0,
                            totalDuration = if (isCurrentPlaying) playerDuration else recording.duration.toInt(),
                            onClick = {
                                if (isCurrentPlaying) {
                                    togglePlayPause()
                                } else {
                                    startPlayback(recording)
                                }
                            },
                            onLongClick = { deletingRecording = recording },
                            onDelete = { deletingRecording = recording },
                            onSeek = { fraction ->
                                val mp = mediaPlayer
                                if (mp != null && isCurrentPlaying) {
                                    val seekTo = (fraction * mp.duration).toInt()
                                    mp.seekTo(seekTo)
                                    playerPosition = seekTo
                                    playerProgress = fraction
                                }
                            }
                        )
                        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingItem(
    recording: CallRecording,
    isPlaying: Boolean,
    isExpanded: Boolean,
    progress: Float,
    currentPos: Int,
    totalDuration: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onSeek: (Float) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isExpanded) AppColors.Surface.copy(alpha = 0.5f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Play/Pause button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) AppColors.Primary.copy(alpha = 0.2f)
                        else AppColors.Surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) AppColors.Primary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.contactName ?: "Unknown",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recording.phoneNumber,
                    color = AppColors.Subtitle,
                    fontSize = 13.sp
                )
                Text(
                    text = dateFormatter.format(Date(recording.recordedAt)),
                    color = AppColors.SurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(recording.duration),
                    color = AppColors.Subtitle,
                    fontSize = 12.sp
                )
                Text(
                    text = formatFileSize(recording.fileSize),
                    color = AppColors.SurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AppColors.Decline.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded player with seek bar
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp, start = 64.dp)) {
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.Primary,
                        activeTrackColor = AppColors.Primary,
                        inactiveTrackColor = AppColors.SurfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(currentPos),
                        color = AppColors.Subtitle,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatDurationMs(totalDuration),
                        color = AppColors.Subtitle,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "${min}m ${sec}s"
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
        else -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
    }
}
