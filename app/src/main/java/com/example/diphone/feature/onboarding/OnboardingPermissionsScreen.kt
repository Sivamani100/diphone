package com.example.diphone.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.diphone.theme.AppColors

@Composable
fun OnboardingPermissionsScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var allPermissionsGranted by remember { mutableStateOf(false) }

    // Define permissions list first
    val permissions = listOf(
        PermissionInfo(
            title = "📞 Call Management",
            description = "Allow DiPhone to make and receive calls, manage your call log",
            requiredPerms = arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG
            ),
            icon = Icons.Default.Phone,
            details = listOf(
                "✓ Make outgoing calls",
                "✓ Receive incoming calls",
                "✓ Access call history",
                "✓ Record call details"
            )
        ),
        PermissionInfo(
            title = "📋 Contact Access",
            description = "Access your contacts to show caller ID and contact details",
            requiredPerms = arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
            ),
            icon = Icons.Default.Contacts,
            details = listOf(
                "✓ Read contact names",
                "✓ Display caller ID",
                "✓ Auto-complete contacts",
                "✓ Manage blocked contacts"
            )
        ),
        PermissionInfo(
            title = "🎤 Call Recording",
            description = "Record phone calls for future reference and documentation",
            requiredPerms = arrayOf(Manifest.permission.RECORD_AUDIO),
            icon = Icons.Default.Mic,
            details = listOf(
                "✓ Record incoming calls",
                "✓ Record outgoing calls",
                "✓ Audio quality optimization",
                "✓ Save recordings locally"
            )
        ),
        PermissionInfo(
            title = "🔔 Notifications",
            description = "Receive call notifications even when app is closed",
            requiredPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf()
            },
            icon = Icons.Default.Notifications,
            details = listOf(
                "✓ Incoming call alerts",
                "✓ Missed call notifications",
                "✓ Voicemail notifications",
                "✓ Lock screen notifications"
            )
        ),
        PermissionInfo(
            title = "🪟 Overlay Permission",
            description = "Show call screen and notifications over other apps",
            requiredPerms = arrayOf(),
            icon = Icons.Default.Layers,
            details = listOf(
                "✓ Display incoming call UI",
                "✓ Show notifications on top",
                "✓ Return to call while multitasking",
                "✓ Always-on-top call controls"
            ),
            isOverlayPerm = true
        )
    )

    // Permission launcher for all permissions at once
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = results.values.all { it }
        if (allPermissionsGranted) {
            currentStep = permissions.size // Completion screen
        }
    }

    // Notification permission launcher (Android 13+)
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentStep++
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        when {
            currentStep < permissions.size -> {
                val currentPerm = permissions[currentStep]
                PermissionRequestScreen(
                    permissionInfo = currentPerm,
                    currentStep = currentStep,
                    totalSteps = permissions.size,
                    onGranted = {
                        if (currentPerm.isOverlayPerm) {
                            openOverlaySettings(context)
                            currentStep++
                        } else if (currentPerm.requiredPerms.isNotEmpty()) {
                            if (currentPerm.requiredPerms.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                multiPermissionLauncher.launch(currentPerm.requiredPerms)
                                currentStep++
                            }
                        } else {
                            currentStep++
                        }
                    },
                    onSkip = {
                        currentStep++
                    }
                )
            }
            else -> {
                CompletionScreen(
                    onComplete = {
                        com.example.diphone.core.data.system.OnboardingManager(context).completeOnboarding()
                        onOnboardingComplete()
                    }
                )
            }
        }
    }
}

data class PermissionInfo(
    val title: String,
    val description: String,
    val requiredPerms: Array<String>,
    val icon: ImageVector,
    val details: List<String>,
    val isOverlayPerm: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionInfo

        if (title != other.title) return false
        if (description != other.description) return false
        if (!requiredPerms.contentEquals(other.requiredPerms)) return false
        if (icon != other.icon) return false
        if (details != other.details) return false
        if (isOverlayPerm != other.isOverlayPerm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + requiredPerms.contentHashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + details.hashCode()
        result = 31 * result + isOverlayPerm.hashCode()
        return result
    }
}

@Composable
fun PermissionRequestScreen(
    permissionInfo: PermissionInfo,
    currentStep: Int,
    totalSteps: Int,
    onGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStep + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = AppColors.Primary,
                trackColor = AppColors.Surface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step counter
            Text(
                "Step ${currentStep + 1} of $totalSteps",
                fontSize = 12.sp,
                color = AppColors.Subtitle,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = permissionInfo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = AppColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                permissionInfo.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                permissionInfo.description,
                fontSize = 16.sp,
                color = AppColors.Subtitle,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Details card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Surface)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Why we need this:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    permissionInfo.details.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                detail,
                                fontSize = 14.sp,
                                color = AppColors.Subtitle,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        if (permissionInfo.isOverlayPerm) "Enable in Settings" else "Allow",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        "Skip for now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Subtitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CompletionScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success animation icon
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(AppColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = AppColors.Primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "All set! 🎉",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "DiPhone is ready to give you the best calling experience with all features enabled.",
                    fontSize = 16.sp,
                    color = AppColors.Subtitle,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Features grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.Surface)
                        .padding(16.dp)
                ) {
                    listOf(
                        "📲 Beautiful call interface",
                        "🎤 Crystal clear recording",
                        "🔔 Smart notifications",
                        "⚡ Lightning fast response"
                    ).forEach { feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                feature,
                                fontSize = 14.sp,
                                color = AppColors.Subtitle
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "Start Using DiPhone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("OnboardingScreen", "Error opening overlay settings: ${e.message}")
    }
}
