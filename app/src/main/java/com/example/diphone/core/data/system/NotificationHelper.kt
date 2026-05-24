package com.example.diphone.core.data.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Log
import com.example.diphone.MainActivity
import com.example.diphone.feature.incall.InCallActivity
import com.example.diphone.service.CallActionReceiver
import com.example.diphone.service.CallActions
import com.example.diphone.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val MISSED_CALL_GROUP_KEY = "missed_calls_group"

    /**
     * Check if we have permission to post notifications (Android 13+)
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true  // Pre-Android 13 doesn't require explicit runtime permission
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 1: INCOMING CALL
    // ═════════════════════════════════════════════════════════════════

    fun showIncomingCallNotification(
        context: Context,
        number: String,
        name: String?,
        isBlocked: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val displayName = if (name.isNullOrBlank()) number else name

            // Full-screen intent → opens InCallActivity
            val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Answer action
            val answerIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActions.ACTION_ANSWER
            }
            val answerPendingIntent = PendingIntent.getBroadcast(
                context, 1, answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decline action
            val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActions.ACTION_DECLINE
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                context, 2, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_INCOMING_CALL)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(displayName)
                .setContentText("Incoming call…")
                .setSubText(number)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setTimeoutAfter(60_000)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF00C853.toInt())
                .setColorized(true)
                .setVibrate(longArrayOf(0, 500, 500, 500, 500, 500))
                .apply {
                    if (!isBlocked) {
                        addAction(
                            android.R.drawable.ic_menu_call,
                            "Answer",
                            answerPendingIntent
                        )
                    }
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Decline",
                        declinePendingIntent
                    )
                }
                .build()

            notificationManager.notify(NotificationIds.INCOMING_CALL, notification)
            Log.d(TAG, "Incoming call notification posted: $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing incoming call notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 2: ONGOING CALL (In-Progress Call)
    // ═════════════════════════════════════════════════════════════════

    fun showOngoingCallNotification(
        context: Context,
        number: String,
        name: String?,
        callDurationMs: Long,
        isMuted: Boolean = false,
        isOnHold: Boolean = false,
        isRecording: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, call notification may not display")
            // Don't return - try to show anyway, some devices allow it
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val displayName = if (name.isNullOrBlank()) number else name

            // Return to call intent
            val returnToCallIntent = Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val returnToCallPendingIntent = PendingIntent.getActivity(
                context, 100, returnToCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Mute/Unmute action
            val muteIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = if (isMuted) CallActions.ACTION_UNMUTE else CallActions.ACTION_MUTE
            }
            val mutePendingIntent = PendingIntent.getBroadcast(
                context, 101, muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Speaker action
            val speakerIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActions.ACTION_TOGGLE_SPEAKER
            }
            val speakerPendingIntent = PendingIntent.getBroadcast(
                context, 102, speakerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // End call action
            val endCallIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActions.ACTION_END_CALL
            }
            val endCallPendingIntent = PendingIntent.getBroadcast(
                context, 103, endCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Hold/Resume action
            val holdIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = if (isOnHold) CallActions.ACTION_RESUME else CallActions.ACTION_HOLD
            }
            val holdPendingIntent = PendingIntent.getBroadcast(
                context, 104, holdIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val statusText = when {
                isOnHold -> "On hold"
                isMuted -> "🔇 Muted"
                isRecording -> "🔴 Recording"
                else -> "Active"
            }

            val builder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_ONGOING_CALL)
                .setSmallIcon(
                    when {
                        isRecording -> android.R.drawable.ic_dialog_info
                        isOnHold -> android.R.drawable.ic_menu_rotate
                        else -> android.R.drawable.ic_menu_call
                    }
                )
                .setContentTitle(displayName)
                .setContentText(statusText)
                .setSubText(number)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - callDurationMs)
                .setContentIntent(returnToCallPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF00C853.toInt())
                .setColorized(true)
                .setSilent(true)  // Don't make sound for ongoing notification

            // Action buttons based on state
            if (isOnHold) {
                builder.addAction(android.R.drawable.ic_media_play, "Resume", holdPendingIntent)
            } else {
                builder.addAction(
                    android.R.drawable.ic_menu_manage,
                    if (isMuted) "Unmute" else "Mute",
                    mutePendingIntent
                )
                builder.addAction(android.R.drawable.ic_menu_info_details, "Speaker", speakerPendingIntent)
            }
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", endCallPendingIntent)

            val notification = builder.build()
            
            // Post with maximum priority - try multiple times if needed
            try {
                notificationManager.notify(NotificationIds.ONGOING_CALL, notification)
                Log.d(TAG, "Ongoing call notification posted successfully: $displayName")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException posting ongoing call notification - user may need to enable permissions", e)
                // Still log this prominently so we know permissions are needed
                android.util.Log.w("DiPhone", "CRITICAL: POST_NOTIFICATIONS permission denied. Users won't see ongoing call notification!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing ongoing call notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 3: MISSED CALL
    // ═════════════════════════════════════════════════════════════════

    fun showMissedCallNotification(
        context: Context,
        number: String,
        name: String?,
        callTime: Long = System.currentTimeMillis()
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val displayName = if (name.isNullOrBlank()) number else name
            val notificationId = NotificationIds.MISSED_CALL_BASE + number.hashCode()

            // Call back action
            val callBackIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callBackPendingIntent = PendingIntent.getActivity(
                context, notificationId, callBackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Message action
            val messageIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", "")
            }
            val messagePendingIntent = PendingIntent.getActivity(
                context, notificationId + 1000, messageIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Open call log
            val openCallLogIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_TAB", "CALLS")
                putExtra("HIGHLIGHT_NUMBER", number)
            }
            val openCallLogPendingIntent = PendingIntent.getActivity(
                context, notificationId + 2000, openCallLogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MISSED_CALL)
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setContentTitle(displayName)
                .setContentText("Missed call")
                .setSubText(number)
                .setWhen(callTime)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setAutoCancel(true)
                .setContentIntent(openCallLogPendingIntent)
                .setGroup(MISSED_CALL_GROUP_KEY)
                .setNumber(1)
                .addAction(android.R.drawable.ic_menu_call, "Call back", callBackPendingIntent)
                .addAction(android.R.drawable.ic_menu_send, "Message", messagePendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setColor(0xFFFF6D00.toInt())
                .build()

            notificationManager.notify(notificationId, notification)
            BadgeManager.incrementBadge(context)
            Log.d(TAG, "Missed call notification posted: $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing missed call notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 4: BLOCKED CALL
    // ═════════════════════════════════════════════════════════════════

    fun showBlockedCallNotification(
        context: Context,
        number: String,
        name: String? = null,
        reason: String = "Blocked"
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val displayName = if (name.isNullOrBlank()) number else name

            // View blocked calls
            val viewBlockedIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_TAB", "BLOCKED")
            }
            val viewBlockedPendingIntent = PendingIntent.getActivity(
                context, 3001, viewBlockedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_BLOCKED_CALL)
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setContentTitle("Blocked call")
                .setContentText(displayName)
                .setSubText(reason)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(viewBlockedPendingIntent)
                .setColor(0xFFCC0000.toInt())
                .build()

            notificationManager.notify(NotificationIds.BLOCKED_CALL, notification)
            Log.d(TAG, "Blocked call notification posted: $displayName ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing blocked call notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 5: BLOCKED MESSAGE
    // ═════════════════════════════════════════════════════════════════

    fun showBlockedMessageNotification(
        context: Context,
        number: String,
        name: String? = null,
        messagePreview: String = ""
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val displayName = if (name.isNullOrBlank()) number else name

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_BLOCKED_MESSAGE)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Blocked message")
                .setContentText(displayName)
                .setStyle(NotificationCompat.BigTextStyle().bigText("\"$messagePreview\""))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(0xFFCC0000.toInt())
                .build()

            notificationManager.notify(NotificationIds.BLOCKED_MESSAGE, notification)
            Log.d(TAG, "Blocked message notification posted: $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing blocked message notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 6: CALL RECORDING
    // ═════════════════════════════════════════════════════════════════

    fun showRecordingNotification(
        context: Context,
        name: String,
        number: String,
        durationMs: Long
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val notificationManager = NotificationManagerCompat.from(context)

            val stopRecordingIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = "com.example.diphone.action.STOP_RECORDING"
            }
            val stopRecordingPendingIntent = PendingIntent.getBroadcast(
                context, 4001, stopRecordingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CALL_RECORDING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔴 Recording call")
                .setContentText("$name · $number")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopRecordingPendingIntent)
                .setColor(0xFFCC0000.toInt())
                .build()

            notificationManager.notify(NotificationIds.RECORDING, notification)
            Log.d(TAG, "Recording notification posted")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing recording notification", e)
        }
    }

    fun showRecordingCompletedNotification(
        context: Context,
        name: String,
        number: String,
        durationMs: Long,
        fileSize: String
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val notificationManager = NotificationManagerCompat.from(context)

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CALL_RECORDING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Recording saved")
                .setContentText("$name · ${formatDuration(durationMs)}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$name\n${formatDuration(durationMs)} · $fileSize\n$number"))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setColor(0xFF00C853.toInt())
                .build()

            notificationManager.notify(NotificationIds.RECORDING, notification)
            Log.d(TAG, "Recording completed notification posted")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing recording completed notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 7: VOICEMAIL
    // ═════════════════════════════════════════════════════════════════

    fun showVoicemailNotification(
        context: Context,
        count: Int = 1
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val notificationManager = NotificationManagerCompat.from(context)

            val callVoicemailIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:**4")
            }
            val callVoicemailPendingIntent = PendingIntent.getActivity(
                context, 5001, callVoicemailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = if (count == 1) "You have 1 new voicemail message" else "You have $count new voicemail messages"

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_VOICEMAIL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("📬 New voicemail")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNumber(count)
                .addAction(android.R.drawable.ic_menu_call, "Call voicemail", callVoicemailPendingIntent)
                .setAutoCancel(true)
                .setColor(0xFF2196F3.toInt())
                .build()

            notificationManager.notify(NotificationIds.VOICEMAIL, notification)
            Log.d(TAG, "Voicemail notification posted: $count messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing voicemail notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPE 8: CALL REMINDER
    // ═════════════════════════════════════════════════════════════════

    fun showCallReminderNotification(
        context: Context,
        name: String,
        number: String
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val notificationManager = NotificationManagerCompat.from(context)

            val callNowIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callNowPendingIntent = PendingIntent.getActivity(
                context, 6001, callNowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = "com.example.diphone.action.SNOOZE_REMINDER"
                putExtra("PHONE_NUMBER", number)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, 6002, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CALL_REMINDER)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⏰ Call reminder")
                .setContentText("You wanted to call back $name")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_call, "Call now", callNowPendingIntent)
                .addAction(android.R.drawable.ic_menu_more, "Snooze 10 min", snoozePendingIntent)
                .setColor(0xFFFFC107.toInt())
                .build()

            notificationManager.notify(NotificationIds.CALL_REMINDER, notification)
            Log.d(TAG, "Call reminder notification posted: $name")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing call reminder notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // NOTIFICATION CANCELLATION
    // ═════════════════════════════════════════════════════════════════

    fun cancelCallNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NotificationIds.INCOMING_CALL)
            Log.d(TAG, "Incoming call notification cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling call notification", e)
        }
    }

    fun cancelOngoingCallNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NotificationIds.ONGOING_CALL)
            Log.d(TAG, "Ongoing call notification cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling ongoing call notification", e)
        }
    }

    fun cancelMissedCallNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NotificationIds.MISSED_CALL_GROUP)
            Log.d(TAG, "Missed call notifications cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling missed call notification", e)
        }
    }

    fun cancelRecordingNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NotificationIds.RECORDING)
            Log.d(TAG, "Recording notification cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording notification", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═════════════════════════════════════════════════════════════════

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
            minutes > 0 -> "%02d:%02d".format(minutes, seconds % 60)
            else -> "00:%02d".format(seconds)
        }
    }
}
