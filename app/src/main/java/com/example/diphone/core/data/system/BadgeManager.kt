package com.example.diphone.core.data.system

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.leolin.shortcutbadger.ShortcutBadger
import android.util.Log

object BadgeManager {

    private const val TAG = "BadgeManager"

    /**
     * Update badge count to show missed calls + voicemails
     * Call this whenever missed calls or voicemail state changes
     */
    fun updateBadge(context: Context, missedCallCount: Int, voicemailCount: Int = 0) {
        val totalCount = missedCallCount + voicemailCount
        
        if (totalCount > 0) {
            applyBadge(context, totalCount)
            Log.d(TAG, "Badge updated to: $totalCount (missed: $missedCallCount, voicemail: $voicemailCount)")
        } else {
            clearBadge(context)
            Log.d(TAG, "Badge cleared")
        }
    }

    /**
     * Increment badge by 1 (for single missed call)
     */
    fun incrementBadge(context: Context) {
        try {
            val prefs = context.getSharedPreferences("badge_prefs", Context.MODE_PRIVATE)
            val currentCount = prefs.getInt("badge_count", 0)
            val newCount = currentCount + 1
            prefs.edit().putInt("badge_count", newCount).apply()
            applyBadge(context, newCount)
            Log.d(TAG, "Badge incremented to: $newCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing badge", e)
        }
    }

    /**
     * Clear badge completely
     */
    fun clearBadge(context: Context) {
        try {
            val prefs = context.getSharedPreferences("badge_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("badge_count", 0).apply()
            
            // Android 8+ native badge via notification
            NotificationManagerCompat.from(context).cancel(NotificationIds.MISSED_CALL_GROUP)
            
            // Third-party launchers
            try {
                ShortcutBadger.removeCount(context)
            } catch (e: Exception) {
                Log.w(TAG, "ShortcutBadger not available on this launcher")
            }
            
            Log.d(TAG, "Badge cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing badge", e)
        }
    }

    /**
     * Get current badge count from preference
     */
    fun getCurrentBadgeCount(context: Context): Int {
        val prefs = context.getSharedPreferences("badge_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("badge_count", 0)
    }

    private fun applyBadge(context: Context, count: Int) {
        try {
            // Save to preference for persistence
            val prefs = context.getSharedPreferences("badge_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("badge_count", count).apply()
            
            // Third-party launchers (Samsung, Xiaomi, OPPO, OnePlus, Huawei, etc.)
            try {
                ShortcutBadger.applyCount(context, count)
                Log.d(TAG, "ShortcutBadger applied count: $count")
            } catch (e: Exception) {
                Log.w(TAG, "ShortcutBadger not available on this launcher")
            }
            
            // Android 8+ stock Android via notification group summary
            // Badge is read from setNumber() on notification
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying badge", e)
        }
    }
}

/**
 * Notification IDs — must be unique per notification type
 */
object NotificationIds {
    const val INCOMING_CALL = 1001
    const val ONGOING_CALL = 1002
    const val MISSED_CALL_BASE = 2000  // Per missed call: 2000, 2001, 2002...
    const val MISSED_CALL_GROUP = 2999  // Group summary
    const val BLOCKED_CALL = 3001
    const val BLOCKED_MESSAGE = 3002
    const val RECORDING = 4001
    const val VOICEMAIL = 5001
    const val CALL_REMINDER = 6001
    const val AUTO_ANSWER_COUNTDOWN = 7001
    const val SYSTEM = 8001
}
