package com.example.diphone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.diphone.core.data.system.CallManager

/**
 * Handles all notification action buttons:
 * - Answer / Decline
 * - Mute / Unmute
 * - Hold / Resume
 * - Speaker toggle
 * - End call
 * - Call back
 * - Message
 * - Unblock
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val callId = intent.getStringExtra("CALL_ID")

        Log.d("CallActionReceiver", "Action: $action, CallId: $callId")

        try {
            when (action) {
                CallActions.ACTION_ANSWER -> {
                    CallManager.accept(context)
                }

                CallActions.ACTION_DECLINE -> {
                    CallManager.decline(context)
                }

                CallActions.ACTION_END_CALL -> {
                    CallManager.endCall(context)
                }

                CallActions.ACTION_MUTE -> {
                    CallManager.toggleMute()
                }

                CallActions.ACTION_UNMUTE -> {
                    CallManager.toggleMute()
                }

                CallActions.ACTION_HOLD -> {
                    CallManager.toggleHold()
                }

                CallActions.ACTION_RESUME -> {
                    CallManager.toggleHold()
                }

                CallActions.ACTION_TOGGLE_SPEAKER -> {
                    CallManager.toggleSpeaker()
                }

                else -> {
                    Log.w("CallActionReceiver", "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            Log.e("CallActionReceiver", "Error handling action: $action", e)
        }
    }
}

/**
 * Action constants for broadcast intents from notifications
 */
object CallActions {
    const val ACTION_ANSWER = "com.example.diphone.action.ANSWER"
    const val ACTION_DECLINE = "com.example.diphone.action.DECLINE"
    const val ACTION_END_CALL = "com.example.diphone.action.END_CALL"
    const val ACTION_MUTE = "com.example.diphone.action.MUTE"
    const val ACTION_UNMUTE = "com.example.diphone.action.UNMUTE"
    const val ACTION_HOLD = "com.example.diphone.action.HOLD"
    const val ACTION_RESUME = "com.example.diphone.action.RESUME"
    const val ACTION_TOGGLE_SPEAKER = "com.example.diphone.action.TOGGLE_SPEAKER"
}
