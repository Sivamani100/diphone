package com.example.diphone.core.data.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationChannels {

    // Channel IDs
    const val CHANNEL_INCOMING_CALL = "ch_incoming_call"
    const val CHANNEL_ONGOING_CALL = "ch_ongoing_call"
    const val CHANNEL_MISSED_CALL = "ch_missed_call"
    const val CHANNEL_BLOCKED_CALL = "ch_blocked_call"
    const val CHANNEL_BLOCKED_MESSAGE = "ch_blocked_message"
    const val CHANNEL_CALL_RECORDING = "ch_call_recording"
    const val CHANNEL_VOICEMAIL = "ch_voicemail"
    const val CHANNEL_CALL_REMINDER = "ch_call_reminder"
    const val CHANNEL_SYSTEM = "ch_system"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. INCOMING CALL — Highest importance, full screen intent
        NotificationChannel(
            CHANNEL_INCOMING_CALL,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming phone calls"
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                setSound(ringtoneUri, audioAttrs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 500)
            lockscreenVisibility = 1  // VISIBILITY_PUBLIC
            setBypassDnd(false)
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        // 2. ONGOING CALL — HIGH importance so it stays visible as persistent notification
        NotificationChannel(
            CHANNEL_ONGOING_CALL,
            "Ongoing Call",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows while a call is in progress"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = 1  // VISIBILITY_PUBLIC
            setShowBadge(false)
            setBypassDnd(true)  // Bypass DND while call is active
        }.also { manager.createNotificationChannel(it) }

        // 3. MISSED CALL — High importance, badge, sound
        NotificationChannel(
            CHANNEL_MISSED_CALL,
            "Missed Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for missed phone calls"
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(notifUri, audioAttrs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            lockscreenVisibility = 0  // VISIBILITY_PRIVATE
            setShowBadge(true)
        }.also { manager.createNotificationChannel(it) }

        // 4. BLOCKED CALL — Default importance, no sound
        NotificationChannel(
            CHANNEL_BLOCKED_CALL,
            "Blocked Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when a call is blocked"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = 0  // VISIBILITY_PRIVATE
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        // 5. BLOCKED MESSAGE — Default importance, no sound
        NotificationChannel(
            CHANNEL_BLOCKED_MESSAGE,
            "Blocked Messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when a message is blocked"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = 0  // VISIBILITY_PRIVATE
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        // 6. CALL RECORDING — Low importance, persistent while recording
        NotificationChannel(
            CHANNEL_CALL_RECORDING,
            "Call Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows recording status during a call"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = 1  // VISIBILITY_PUBLIC
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        // 7. VOICEMAIL — High importance, badge, sound
        NotificationChannel(
            CHANNEL_VOICEMAIL,
            "Voicemail",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New voicemail messages"
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(notifUri, audioAttrs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            enableVibration(true)
            lockscreenVisibility = 0  // VISIBILITY_PRIVATE
            setShowBadge(true)
        }.also { manager.createNotificationChannel(it) }

        // 8. CALL REMINDER — Default importance, sound
        NotificationChannel(
            CHANNEL_CALL_REMINDER,
            "Call Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to call back missed calls"
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(notifUri, audioAttrs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            enableVibration(true)
            lockscreenVisibility = 0  // VISIBILITY_PRIVATE
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        // 9. SYSTEM — Low importance, no sound
        NotificationChannel(
            CHANNEL_SYSTEM,
            "System",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "App system messages"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }.also { manager.createNotificationChannel(it) }

        android.util.Log.d("NotificationChannels", "All 9 notification channels created")
    }
}
