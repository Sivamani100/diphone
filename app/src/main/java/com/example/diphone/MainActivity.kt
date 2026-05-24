package com.example.diphone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.diphone.theme.DiPhoneTheme
import com.example.diphone.core.data.system.BadgeHelper
import com.example.diphone.core.data.system.OnboardingManager

class MainActivity : ComponentActivity() {

    private val requiredPermissions = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.WRITE_CONTACTS)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.WRITE_CALL_LOG)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // After permissions are answered, refresh repositories with real data
        val container = DiPhoneApp.getContainer(this)
        container.contactsRepository.refreshSystemContacts()
        container.callLogRepository.refreshSystemLogs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if onboarding has been completed
        val onboardingManager = OnboardingManager(this)
        val isFirstInstall = onboardingManager.isFirstInstall()

        // Clear badge when app opens
        BadgeHelper.removeBadge(this)

        // Check if this intent should show missed calls
        val showMissedCalls = intent?.getBooleanExtra("show_missed_calls", false) ?: false

        setContent {
            DiPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show onboarding only on first install
                    if (isFirstInstall) {
                        com.example.diphone.feature.onboarding.OnboardingPermissionsScreen(
                            onOnboardingComplete = {
                                // After onboarding, request any critical background permissions
                                requestMissingPermissions()
                                // Recreate activity to show main app
                                recreate()
                            }
                        )
                    } else {
                        MainNavigation(showMissedCallsTab = showMissedCalls)
                    }
                }
            }
        }
    }

    private fun requestMissingPermissions() {
        val missing = requiredPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing)
        }
    }
}
