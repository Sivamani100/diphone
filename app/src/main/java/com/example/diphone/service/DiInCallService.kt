package com.example.diphone.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telecom.Call
import android.telecom.InCallService
import com.example.diphone.core.data.system.CallManager
import com.example.diphone.core.data.system.NotificationHelper
import com.example.diphone.feature.incall.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiInCallService : InCallService() {

    // Per-call callback to mirror state changes into CallManager
    private val callCallbacks = mutableMapOf<Call, Call.Callback>()

    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CallActions.ACTION_ANSWER -> {
                    CallManager.accept(context)
                }
                CallActions.ACTION_DECLINE -> {
                    CallManager.decline(context)
                }
                CallActions.ACTION_MUTE -> {
                    CallManager.toggleMute()
                }
                CallActions.ACTION_UNMUTE -> {
                    CallManager.toggleMute()
                }
                CallActions.ACTION_TOGGLE_SPEAKER -> {
                    CallManager.toggleSpeaker()
                }
                CallActions.ACTION_HOLD -> {
                    CallManager.toggleHold()
                }
                CallActions.ACTION_RESUME -> {
                    CallManager.toggleHold()
                }
                CallActions.ACTION_END_CALL -> {
                    CallManager.endCall(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val filter = IntentFilter().apply {
            addAction(CallActions.ACTION_ANSWER)
            addAction(CallActions.ACTION_DECLINE)
            addAction(CallActions.ACTION_MUTE)
            addAction(CallActions.ACTION_UNMUTE)
            addAction(CallActions.ACTION_TOGGLE_SPEAKER)
            addAction(CallActions.ACTION_HOLD)
            addAction(CallActions.ACTION_RESUME)
            addAction(CallActions.ACTION_END_CALL)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(actionReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        CallManager.inCallService = this
        CallManager.appContext = this.applicationContext
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CallManager.inCallService = null
        CallManager.activeCalls.clear()
        return super.onUnbind(intent)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.activeCalls.add(call)
        CallManager.updateActiveCallsCount()

        // Create a dedicated callback for this call object
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                handleCallState(call, state)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                // Re-evaluate state in case details (like caller name) updated
                handleCallState(call, call.state)
            }
        }
        callCallbacks[call] = callback
        call.registerCallback(callback)

        // Mirror the initial state immediately
        handleCallState(call, call.state)

        // Launch our custom InCallActivity to show the call UI
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.activeCalls.remove(call)
        CallManager.updateActiveCallsCount()

        // Unregister the callback
        callCallbacks.remove(call)?.let { call.unregisterCallback(it) }

        // Determine call state before disconnecting
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
        val callDirection = try {
            call.details?.callDirection
        } catch (e: Exception) {
            Call.Details.DIRECTION_INCOMING
        }
        
        val isMissedCall = try {
            callDirection == Call.Details.DIRECTION_INCOMING && 
            call.state != Call.STATE_ACTIVE &&
            call.state != Call.STATE_CONNECTING &&
            call.state != Call.STATE_DIALING
        } catch (e: Exception) {
            false
        }
        
        android.util.Log.d("DiInCallService", "onCallRemoved: number=$number, name=$name, callDirection=$callDirection, state=${call.state}, isMissedCall=$isMissedCall")
        
        // Show missed call notification BEFORE canceling incoming notification
        if (isMissedCall) {
            android.util.Log.d("DiInCallService", "Showing missed call notification for: $number")
            NotificationHelper.showMissedCallNotification(this, number, name)
        }
        
        // Force disconnect state so InCallActivity closes
        CallManager.setDisconnected(number, name, "Ended")
        
        // Cancel both incoming and ongoing call notifications
        NotificationHelper.cancelCallNotification(this)
        NotificationHelper.cancelOngoingCallNotification(this)
    }

    private fun handleCallState(call: Call, state: Int) {
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

        android.util.Log.d("DiInCallService", "handleCallState: state=$state, number=$number, name=$name")

        when (state) {
            Call.STATE_RINGING -> {
                // Incoming call ringing - show notification
                android.util.Log.d("DiInCallService", "STATE_RINGING - showing incoming call notification")
                NotificationHelper.showIncomingCallNotification(this, number, name, isBlocked = false)
                CallManager.setIncomingCall(number, name, call)
            }

            Call.STATE_DIALING,
            Call.STATE_CONNECTING,
            Call.STATE_SELECT_PHONE_ACCOUNT,
            Call.STATE_SIMULATED_RINGING -> {
                // Outgoing call being placed
                android.util.Log.d("DiInCallService", "STATE_DIALING/CONNECTING - setting dialing state")
                CallManager.setDialingCall(number, name, call)
            }

            Call.STATE_ACTIVE -> {
                // Call is connected and active - show persistent ongoing call notification
                android.util.Log.d("DiInCallService", "STATE_ACTIVE - showing ongoing call notification")
                NotificationHelper.cancelCallNotification(this)  // Cancel incoming notification
                
                // Show ongoing call notification immediately so user can access it even after swiping
                val callDurationMs = try {
                    val startTime = CallManager.getCallStartTime()
                    if (startTime > 0L) System.currentTimeMillis() - startTime else 0L
                } catch (e: Exception) {
                    0L
                }
                
                NotificationHelper.showOngoingCallNotification(
                    context = this,
                    number = number,
                    name = name,
                    callDurationMs = callDurationMs,
                    isMuted = CallManager.isMutedFlow.value,
                    isOnHold = CallManager.isHeldFlow.value,
                    isRecording = CallManager.isRecordingFlow.value
                )
                
                CallManager.setActiveCall(number, name, call)
            }

            Call.STATE_HOLDING -> {
                // Call placed on hold — reflect in the active state and update notification
                android.util.Log.d("DiInCallService", "STATE_HOLDING - call on hold")
                CallManager.onCallHeld()
                
                // Update ongoing notification to show on-hold status
                val callDurationMs = try {
                    val startTime = CallManager.getCallStartTime()
                    if (startTime > 0L) System.currentTimeMillis() - startTime else 0L
                } catch (e: Exception) {
                    0L
                }
                NotificationHelper.showOngoingCallNotification(
                    context = this,
                    number = number,
                    name = name,
                    callDurationMs = callDurationMs,
                    isMuted = CallManager.isMutedFlow.value,
                    isOnHold = true,
                    isRecording = CallManager.isRecordingFlow.value
                )
            }

            Call.STATE_DISCONNECTING,
            Call.STATE_DISCONNECTED -> {
                val disconnectCause = try {
                    call.details?.disconnectCause?.reason ?: "Ended"
                } catch (e: Exception) {
                    "Ended"
                }
                
                // Cancel the incoming call notification
                NotificationHelper.cancelCallNotification(this)
                
                CallManager.setDisconnected(number, name, disconnectCause)
            }
        }
    }
}

