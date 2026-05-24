package com.example.diphone.core.data.system

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import android.util.Log

object DndManager {

    private const val TAG = "DndManager"

    /**
     * Check if a call should be allowed to ring/vibrate during DND
     * Respects both system DND + our in-app DND settings
     */
    fun isCallAllowedToRing(
        context: Context,
        callerNumber: String,
        isVip: Boolean = false
    ): Boolean {
        // Check system DND first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val currentInterruptionFilter = notificationManager.currentInterruptionFilter

            when (currentInterruptionFilter) {
                // Total silence
                NotificationManager.INTERRUPTION_FILTER_NONE -> {
                    Log.d(TAG, "System DND: NONE (total silence) — VIP override: $isVip")
                    return isVip  // VIP always gets through
                }

                // Alarms only
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> {
                    Log.d(TAG, "System DND: ALARMS only — call blocked")
                    return false
                }

                // Priority mode — check specific rules
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val policy = notificationManager.notificationPolicy
                            // In Android P+, check if calls are allowed via priority policy
                            val callsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                policy.priorityCategories != 0  // If any category is set, calls are typically allowed
                            } else {
                                true  // Pre-P: assume calls are allowed in PRIORITY mode
                            }

                            Log.d(TAG, "System DND: PRIORITY — calls allowed in policy: $callsAllowed, isVip: $isVip")
                            return callsAllowed || isVip
                        } catch (e: Exception) {
                            Log.w(TAG, "Error checking priority policy", e)
                            return isVip
                        }
                    }
                    return isVip
                }

                // All notifications
                NotificationManager.INTERRUPTION_FILTER_ALL -> {
                    Log.d(TAG, "System DND: ALL notifications allowed")
                    // Fall through to in-app DND check
                }

                else -> {
                    Log.d(TAG, "System DND: UNKNOWN state")
                    return true
                }
            }
        }

        // Check in-app DND (on top of system DND)
        if (isInAppDndActive(context)) {
            Log.d(TAG, "In-app DND active — VIP override: $isVip")
            // Check if VIP can override in-app DND
            return if (isVip) {
                val prefs = context.getSharedPreferences("dnd_prefs", Context.MODE_PRIVATE)
                prefs.getBoolean("dnd_allow_vip", true)  // Default: true (VIP always rings)
            } else {
                false
            }
        }

        Log.d(TAG, "DND check passed — call will ring")
        return true
    }

    /**
     * Check if in-app DND is currently active (based on time)
     */
    private fun isInAppDndActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("dnd_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("dnd_enabled", false)

        if (!enabled) return false

        try {
            val startMinutes = prefs.getInt("dnd_start_minutes", 23 * 60)  // 23:00
            val endMinutes = prefs.getInt("dnd_end_minutes", 7 * 60)       // 07:00

            val calendar = java.util.Calendar.getInstance()
            val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                    calendar.get(java.util.Calendar.MINUTE)

            // If start > end (e.g., 23:00 to 07:00 crosses midnight)
            return if (startMinutes > endMinutes) {
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                // Normal range (e.g., 13:00 to 18:00)
                currentMinutes >= startMinutes && currentMinutes < endMinutes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking in-app DND", e)
            return false
        }
    }

    /**
     * Whether to silence notification (no sound/vibration) during DND
     * Notification still shows, but silently
     */
    fun shouldSilenceNotification(context: Context): Boolean {
        if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }
        return false
    }
}
