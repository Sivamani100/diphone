package com.example.diphone.feature.incall

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.system.CallManager
import com.example.diphone.core.data.system.CallState
import com.example.diphone.theme.DiPhoneTheme
import com.example.diphone.core.data.system.SoundHelper
import com.example.diphone.core.data.system.PhoneCallHelper
import kotlin.math.sin

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Turn screen on and show over lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            DiPhoneTheme {
                val state by CallManager.callState.collectAsState()
                val activeCallsCount by CallManager.activeCallsCount.collectAsState()
                val isMuted by CallManager.isMutedFlow.collectAsState()
                val isSpeakerOn by CallManager.isSpeakerOnFlow.collectAsState()
                val isHeld by CallManager.isHeldFlow.collectAsState()
                val isRecording by CallManager.isRecordingFlow.collectAsState()
                val recordingDuration by CallManager.recordingSecondsFlow.collectAsState()
                
                // If call state transitions to Idle, close the screen
                LaunchedEffect(state) {
                    if (state is CallState.Idle) {
                        CallManager.stopRingtone()
                        finish()
                    }
                }

                InCallScreen(
                    state = state,
                    activeCallsCount = activeCallsCount,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isHeld = isHeld,
                    isRecording = isRecording,
                    recordingDuration = recordingDuration,
                    onAccept = { CallManager.accept(this) },
                    onDecline = { CallManager.decline(this) },
                    onToggleMute = { CallManager.toggleMute() },
                    onToggleSpeaker = { CallManager.toggleSpeaker() },
                    onToggleHold = { CallManager.toggleHold() },
                    onToggleRecording = { CallManager.toggleRecording(this) },
                    onMergeCalls = { CallManager.mergeCalls() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CallManager.stopRingtone()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.stopRingtone()
    }
}

@Composable
fun InCallScreen(
    state: CallState,
    activeCallsCount: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isHeld: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleRecording: () -> Unit,
    onMergeCalls: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddCallDialog by remember { mutableStateOf(false) }
    var addCallNumber by remember { mutableStateOf("") }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        if (state is CallState.Active) {
            val start = state.startTime
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000
                kotlinx.coroutines.delay(1000)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    if (showAddCallDialog) {
        AlertDialog(
            onDismissRequest = { showAddCallDialog = false },
            title = { Text("Add Call", color = Color.White) },
            text = {
                Column {
                    Text("Enter phone number to dial:", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = addCallNumber,
                        onValueChange = { addCallNumber = it },
                        placeholder = { Text("Phone number", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2C),
                            unfocusedContainerColor = Color(0xFF1C1C1C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addCallNumber.isNotBlank()) {
                            PhoneCallHelper.placeCall(context, addCallNumber)
                            showAddCallDialog = false
                            addCallNumber = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCallDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
    // Elegant dark gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A1C1C), // IncomingCallGradientTop
                        Color(0xFF150A0A),
                        Color(0xFF000000)  // IncomingCallGradientBottom
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Caller Info Header
            val name = when (state) {
                is CallState.Incoming -> state.name ?: "Unknown Number"
                is CallState.Dialing -> state.name ?: "Unknown Number"
                is CallState.Active -> state.name ?: "Unknown Number"
                is CallState.Disconnected -> state.name ?: "Unknown Number"
                else -> "Call"
            }
            val number = when (state) {
                is CallState.Incoming -> state.number
                is CallState.Dialing -> state.number
                is CallState.Active -> state.number
                is CallState.Disconnected -> state.number
                else -> ""
            }
            val status = when (state) {
                is CallState.Incoming -> "Incoming call..."
                is CallState.Dialing -> "Dialing..."
                is CallState.Active -> {
                    if (state.isHeld) "On Hold" else "Connected"
                }
                is CallState.Disconnected -> "Call ended (${state.reason})"
                else -> ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile avatar circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contact Avatar",
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = number,
                    fontSize = 18.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = status,
                    fontSize = 16.sp,
                    color = if (state is CallState.Active && !state.isHeld) Color(0xFF00C853) else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                // Call duration timer (only active)
                if (state is CallState.Active) {
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 2. Animated Audio Waveform (only when active call is connected and not held)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state is CallState.Active && !isHeld) {
                    AudioWaveform()
                }
            }

            // 3. Grid of Call Action Keys (Visible in Active & Dialing Calls)
            if (state is CallState.Active || state is CallState.Dialing) {
                CallControlsGrid(
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isHeld = isHeld,
                    isRecording = isRecording,
                    recordingDuration = recordingDuration,
                    canMerge = activeCallsCount > 1,
                    isDialing = state is CallState.Dialing,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onToggleHold = onToggleHold,
                    onToggleRecording = onToggleRecording,
                    onAddCallClick = { showAddCallDialog = true },
                    onMergeClick = onMergeCalls
                )
            }

            // 4. Primary Call Buttons (Accept/Decline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state is CallState.Incoming) {
                    // Slide to Answer / Tap Buttons
                    CircularCallButton(
                        icon = Icons.Default.Call,
                        label = "Answer",
                        backgroundColor = Color(0xFF00C853), // Green
                        onClick = onAccept
                    )
                    CircularCallButton(
                        icon = Icons.Default.CallEnd,
                        label = "Decline",
                        backgroundColor = Color(0xFFE53935), // Red
                        onClick = onDecline
                    )
                } else if (state is CallState.Active || state is CallState.Dialing || state is CallState.Disconnected) {
                    CircularCallButton(
                        icon = Icons.Default.CallEnd,
                        label = "Decline",
                        backgroundColor = Color(0xFFE53935), // Red
                        onClick = onDecline
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlsGrid(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isHeld: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    canMerge: Boolean,
    isDialing: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleRecording: () -> Unit,
    onAddCallClick: () -> Unit,
    onMergeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlKey(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = "Mute",
                isActive = isMuted,
                onClick = onToggleMute
            )
            ControlKey(
                icon = Icons.Default.VolumeUp,
                label = "Speaker",
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )
            if (canMerge) {
                ControlKey(
                    icon = Icons.Default.MergeType,
                    label = "Merge",
                    isActive = false,
                    onClick = onMergeClick
                )
            } else {
                ControlKey(
                    icon = Icons.Default.Keyboard,
                    label = "Keypad",
                    isActive = false,
                    onClick = { /* Simulated keypad tap */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlKey(
                icon = if (isHeld) Icons.Default.PlayArrow else Icons.Default.Pause,
                label = "Hold",
                isActive = isHeld,
                enabled = !isDialing,
                onClick = onToggleHold
            )
            
            // Call recording status badge
            val recLabel = if (isRecording) {
                 val mins = recordingDuration / 60
                 val secs = recordingDuration % 60
                 String.format("Record (%02d:%02d)", mins, secs)
            } else {
                "Record"
            }
            ControlKey(
                icon = Icons.Default.FiberManualRecord,
                label = recLabel,
                isActive = isRecording,
                enabled = !isDialing,
                activeTint = Color(0xFFE53935), // Red record dot
                onClick = onToggleRecording
            )
            
            ControlKey(
                icon = Icons.Default.Add,
                label = "Add Call",
                isActive = false,
                enabled = !isDialing,
                onClick = onAddCallClick
            )
        }
    }
}

@Composable
fun ControlKey(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    enabled: Boolean = true,
    activeTint: Color = Color(0xFF00C853),
    onClick: () -> Unit
) {
    val opacity = if (enabled) 1.0f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White.copy(alpha = 0.15f * opacity) 
                    else Color(0xFF1E1E1E).copy(alpha = opacity)
                )
                .clickable(enabled = enabled) { 
                    SoundHelper.playButtonClickTone()
                    onClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = (if (isActive) activeTint else Color.White).copy(alpha = opacity),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.LightGray.copy(alpha = opacity),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CircularCallButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable { 
                    SoundHelper.playButtonClickTone()
                    onClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AudioWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val points = 60
        val step = width / points
        
        // Draw 3 layers of smooth sine wave lines overlapping
        for (w in 0..2) {
            val amplitude = when(w) {
                0 -> 40f
                1 -> 25f
                else -> 15f
            }
            val speed = when(w) {
                0 -> 1.0f
                1 -> 1.5f
                else -> 0.8f
            }
            val color = when(w) {
                0 -> Color(0xFF00C853).copy(alpha = 0.5f)
                1 -> Color(0xFF69F0AE).copy(alpha = 0.3f)
                else -> Color.White.copy(alpha = 0.2f)
            }
            
            var prevX = 0f
            var prevY = centerY
            
            for (i in 0..points) {
                val x = i * step
                // Create sine modulation based on distance from center for a clean bubble shape
                val centerWeight = sin((i.toFloat() / points) * Math.PI.toFloat()).toFloat()
                val y = centerY + centerWeight * amplitude * sin(i * 0.15f + phase * speed)
                
                if (i > 0) {
                    drawLine(
                        color = color,
                        start = Offset(prevX, prevY),
                        end = Offset(x, y),
                        strokeWidth = 3f
                    )
                }
                prevX = x
                prevY = y
            }
        }
    }
}
