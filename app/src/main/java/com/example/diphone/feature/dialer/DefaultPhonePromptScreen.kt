package com.example.diphone.feature.dialer

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.diphone.theme.AppColors

@Composable
fun DefaultPhonePromptScreen(
    onDismiss: () -> Unit,
    onRoleGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-dismiss when we come back from the system dialog and the role is now held
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
                } else {
                    val telecomManager =
                        context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                    telecomManager?.defaultDialerPackage == context.packageName
                }
                if (isDefault) {
                    onRoleGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
    ) {
        // Cancel top-right
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Cancel", color = AppColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Titles
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Use System Phone",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "More features, smarter experience",
                    fontSize = 16.sp,
                    color = AppColors.Subtitle
                )
            }

            // Visual Phone Mockup / Illustration Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.Surface)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feature visual overlay 1: Incoming call preview banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2C))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Incoming Call", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Aparna • VoLTE HD", color = AppColors.Subtitle, fontSize = 11.sp)
                        }
                    }

                    // Feature visual overlay 2: Voicemail / Voice Note Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2C))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Voicemail,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Voicemail Box", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("New voice message recorded", color = AppColors.Subtitle, fontSize = 11.sp)
                        }
                    }

                    // Feature visual overlay 3: Call recording waveform
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2C))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = AppColors.Decline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "00:36 ──────── 02:36",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Set default dialer call to action
            Button(
                onClick = {
                    requestDefaultDialerRole(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Set as default calling app",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun requestDefaultDialerRole(context: Context) {
    val activity = context.findActivity()
    if (activity == null) {
        android.util.Log.e("DefaultDialer", "Could not find Activity from context - Settings intent won't open")
        return
    }
    
    try {
        android.util.Log.d("DefaultDialer", "Attempting to open default dialer settings...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager != null) {
                    val isDefault = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                    android.util.Log.d("DefaultDialer", "Is DiPhone default dialer? $isDefault")
                    
                    // Always open the role request intent - let system handle it
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    activity.startActivity(intent)
                    android.util.Log.d("DefaultDialer", "RoleManager intent opened successfully")
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("DefaultDialer", "RoleManager failed: ${e.message}, falling back to Settings")
            }
        }
        
        // Fallback for Android < Q or if RoleManager fails
        android.util.Log.d("DefaultDialer", "Opening Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS")
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
            android.util.Log.d("DefaultDialer", "Settings intent opened successfully")
        } else {
            android.util.Log.w("DefaultDialer", "No app to handle ACTION_MANAGE_DEFAULT_APPS_SETTINGS")
        }
    } catch (e: Exception) {
        android.util.Log.e("DefaultDialer", "Error opening default dialer settings: ${e.message}", e)
    }
}
