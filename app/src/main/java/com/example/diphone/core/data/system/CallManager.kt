package com.example.diphone.core.data.system

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.Call
import com.example.diphone.core.data.local.CallRecording
import com.example.diphone.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first




sealed class CallState {
    object Idle : CallState()
    data class Incoming(val number: String, val name: String?) : CallState()
    data class Dialing(val number: String, val name: String?) : CallState()
    data class Active(
        val number: String,
        val name: String?,
        val startTime: Long,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val isHeld: Boolean = false,
        val isRecording: Boolean = false,
        val recordingDuration: Long = 0L
    ) : CallState()
    data class Disconnected(val number: String, val name: String?, val reason: String) : CallState()
}

object CallManager {
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Reference to the real Telecom Call object set by DiInCallService
    @Volatile var telecomCall: Call? = null
    @Volatile var inCallService: android.telecom.InCallService? = null
    @Volatile var appContext: Context? = null
    val activeCalls = mutableListOf<Call>()
    private val _activeCallsCount = MutableStateFlow(0)
    val activeCallsCount: StateFlow<Int> = _activeCallsCount.asStateFlow()

    fun updateActiveCallsCount() {
        _activeCallsCount.value = activeCalls.size
    }

    // Timer and recording jobs
    private var callTimerJob: Job? = null
    private var recordingTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Transient in-memory call attributes
    private var callStartTime = 0L
    private var callDuration = 0L
    private var wasIncomingCall = false
    val isMutedFlow = MutableStateFlow(false)
    val isSpeakerOnFlow = MutableStateFlow(false)
    val isHeldFlow = MutableStateFlow(false)
    val isRecordingFlow = MutableStateFlow(false)
    val recordingSecondsFlow = MutableStateFlow(0L)

    // Ringtone reference (for stopping on answer/decline)
    private var ringtone: android.media.Ringtone? = null


    fun setIncomingCall(number: String, name: String?, realCall: Call? = null) {
        telecomCall = realCall
        resetCallAttributes()
        wasIncomingCall = true
        
        // Try initial quick lookup in mocks to show something immediately if possible
        var displayName = name
        if (displayName == null || displayName.isBlank()) {
            displayName = appContext?.let { ctx ->
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(ctx)
                    // Try mock lookup first (faster)
                    val mockName = container.contactsRepository.lookupMockContactName(number)
                    mockName ?: number
                } catch (e: Exception) { 
                    number
                }
            } ?: number
        }
        
        // Show immediate incoming call state with current name
        _callState.value = CallState.Incoming(number, displayName)
        
        val context = appContext
        if (context != null) {
            scope.launch(Dispatchers.IO) {
                var ringtoneUri: Uri? = null
                var isBlocked = false
                var routeToVoicemail = false
                var finalDisplayName = displayName
                
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(context)
                    
                    // Check if number is blocked
                    isBlocked = try {
                        container.blockedRepository.isBlocked(number)
                    } catch (e: Exception) {
                        false
                    }
                    
                    if (isBlocked) {
                        // Auto-decline blocked calls and record as missed call
                        setDisconnected(number, finalDisplayName, "Blocked")
                        NotificationHelper.showIncomingCallNotification(context, number, finalDisplayName, isBlocked = true)
                        return@launch
                    }
                    
                    // Full lookup (system contacts first, then mock fallback)
                    try {
                        val resolvedName = container.contactsRepository.lookupContactName(number)
                        if (resolvedName != null && resolvedName.isNotBlank() && resolvedName != "Unknown") {
                            finalDisplayName = resolvedName
                            // Update UI with resolved name
                            val current = _callState.value
                            if (current is CallState.Incoming) {
                                _callState.value = current.copy(name = resolvedName)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // Find contact details for voicemail/ringtone routing
                    val contactId = try {
                        container.contactsRepository.findContactIdByNumber(number)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (contactId != null) {
                        try {
                            val contact = container.contactsRepository.getContactById(contactId)
                            routeToVoicemail = contact?.routeToVoicemail == true
                            if (routeToVoicemail) {
                                setDisconnected(number, finalDisplayName, "Voicemail")
                                NotificationHelper.showIncomingCallNotification(context, number, finalDisplayName, isBlocked = false)
                                return@launch
                            }
                            contact?.customRingtone?.let {
                                if (it.isNotBlank()) {
                                    ringtoneUri = Uri.parse(it)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // Show incoming call notification with resolved name
                    NotificationHelper.showIncomingCallNotification(context, number, finalDisplayName, isBlocked = false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Still show notification even if something failed
                    NotificationHelper.showIncomingCallNotification(context, number, displayName, isBlocked = false)
                }
                
                // Play ringtone
                if (ringtoneUri == null) {
                    try {
                        ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                try {
                    stopRingtone()
                    if (ringtoneUri != null) {
                        ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                        ringtone?.play()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setDialingCall(number: String, name: String?, realCall: Call? = null) {
        telecomCall = realCall
        resetCallAttributes()
        wasIncomingCall = false
        
        var displayName = name
        if (displayName == null || displayName.isBlank()) {
            displayName = appContext?.let { ctx ->
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(ctx)
                    val mockName = container.contactsRepository.lookupMockContactName(number)
                    mockName ?: number
                } catch (e: Exception) { 
                    number
                }
            } ?: number
        }
        
        _callState.value = CallState.Dialing(number, displayName)
        
        val context = appContext
        if (context != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(context)
                    val resolvedName = container.contactsRepository.lookupContactName(number)
                    if (resolvedName != null && resolvedName.isNotBlank() && resolvedName != "Unknown") {
                        val current = _callState.value
                        if (current is CallState.Dialing) {
                            _callState.value = current.copy(name = resolvedName)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setActiveCall(number: String, name: String?, realCall: Call? = null) {
        telecomCall = realCall
        if (_callState.value !is CallState.Active) {
            callStartTime = System.currentTimeMillis()
            isHeldFlow.value = false
        }
        
        var displayName = name ?: when (val curr = _callState.value) {
            is CallState.Incoming -> curr.name
            is CallState.Dialing -> curr.name
            is CallState.Active -> curr.name
            else -> null
        }
        
        if (displayName == null || displayName.isBlank()) {
            displayName = appContext?.let { ctx ->
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(ctx)
                    val mockName = container.contactsRepository.lookupMockContactName(number)
                    mockName ?: number
                } catch (e: Exception) { 
                    number
                }
            } ?: number
        }
        
        _callState.value = CallState.Active(
            number = number,
            name = displayName,
            startTime = callStartTime,
            isMuted = isMutedFlow.value,
            isSpeakerOn = isSpeakerOnFlow.value,
            isHeld = isHeldFlow.value,
            isRecording = isRecordingFlow.value,
            recordingDuration = recordingSecondsFlow.value
        )
        startCallTimer()
        
        val context = appContext
        if (context != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(context)
                    val resolvedName = container.contactsRepository.lookupContactName(number)
                    if (resolvedName != null && resolvedName.isNotBlank() && resolvedName != "Unknown") {
                        val current = _callState.value
                        if (current is CallState.Active) {
                            _callState.value = current.copy(name = resolvedName)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onCallHeld() {
        val current = _callState.value
        isHeldFlow.value = true
        if (current is CallState.Active) {
            _callState.value = current.copy(isHeld = true)
        }
    }

    fun setDisconnected(number: String, name: String?, reason: String) {
        val duration = if (callStartTime > 0) {
            (System.currentTimeMillis() - callStartTime) / 1000
        } else {
            0L
        }
        callDuration = duration

        SoundHelper.playCallEndTone()
        stopTimers()
        stopRingtone()
        telecomCall = null
        
        var displayName = name ?: when (val curr = _callState.value) {
            is CallState.Incoming -> curr.name
            is CallState.Dialing -> curr.name
            is CallState.Active -> curr.name
            else -> null
        }
        
        val context = appContext
        if (context != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val container = com.example.diphone.DiPhoneApp.getContainer(context)
                    var finalName = displayName
                    if (finalName == null || finalName.isBlank()) {
                        finalName = container.contactsRepository.lookupContactName(number)
                    }
                    
                    // Determine call type based on reason and whether it was answered
                    val callType = when {
                        reason == "Blocked" -> 4 // BLOCKED
                        reason == "Voicemail" -> 3 // MISSED (voicemail routing)
                        reason == "Rejected" -> 3 // MISSED
                        reason == "Cancelled" -> 2 // OUTGOING (cancelled)
                        wasIncomingCall && duration > 0 -> 1 // INCOMING (completed)
                        wasIncomingCall && duration == 0L -> 3 // MISSED (incoming but not answered)
                        else -> 2 // OUTGOING
                    }
                    
                    // Add to mock logs which triggers real-time refresh
                    container.callLogRepository.addMockCall(
                        number = number,
                        name = finalName,
                        type = callType,
                        duration = duration
                    )
                    
                    // Note: Missed call notification is now handled in DiInCallService.onCallRemoved
                    // to avoid duplicate notifications
                    
                    // Refresh system logs to ensure UI shows the updated call
                    container.callLogRepository.refreshSystemLogs()
                } catch (e: Exception) {
                    // Silent fail
                }
            }
        }
        
        // Go directly to Idle — InCallActivity observes this and calls finish() immediately
        _callState.value = CallState.Idle
    }

    // ─── User Actions (called from InCallActivity) ───────────────────────────

    fun accept(context: Context) {
        val current = _callState.value
        if (current is CallState.Incoming) {
            stopRingtone()
            vibrate(context, 200)
            SoundHelper.playCallConnectTone()
            try {
                telecomCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Exception) {
                // Telecom call object may already have auto-answered
            }
        }
    }

    fun decline(context: Context) {
        val current = _callState.value
        stopRingtone()
        SoundHelper.playCallEndTone()
        when (current) {
            is CallState.Incoming -> {
                try {
                    telecomCall?.reject(false, null)
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Rejected")
            }
            is CallState.Active -> {
                try {
                    telecomCall?.disconnect()
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Ended")
            }
            is CallState.Dialing -> {
                try {
                    telecomCall?.disconnect()
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Cancelled")
            }
            else -> { /* nothing to do */ }
        }
    }

    fun toggleMute() {
        val current = _callState.value
        isMutedFlow.value = !isMutedFlow.value
        try {
            inCallService?.setMuted(isMutedFlow.value)
        } catch (e: Exception) { /* ignored */ }
        if (current is CallState.Active) {
            _callState.value = current.copy(isMuted = isMutedFlow.value)
            // Update notification to show new mute state
            appContext?.let { ctx ->
                val callDurationMs = if (callStartTime > 0L) System.currentTimeMillis() - callStartTime else 0L
                NotificationHelper.showOngoingCallNotification(
                    context = ctx,
                    number = current.number,
                    name = current.name,
                    callDurationMs = callDurationMs,
                    isMuted = isMutedFlow.value,
                    isOnHold = isHeldFlow.value,
                    isRecording = isRecordingFlow.value
                )
            }
        }
    }

    fun toggleSpeaker() {
        val current = _callState.value
        isSpeakerOnFlow.value = !isSpeakerOnFlow.value
        try {
            if (isSpeakerOnFlow.value) {
                inCallService?.setAudioRoute(android.telecom.CallAudioState.ROUTE_SPEAKER)
            } else {
                inCallService?.setAudioRoute(android.telecom.CallAudioState.ROUTE_WIRED_OR_EARPIECE)
            }
        } catch (e: Exception) { /* ignored */ }
        if (current is CallState.Active) {
            _callState.value = current.copy(isSpeakerOn = isSpeakerOnFlow.value)
            // Update notification (speaker state doesn't change visual status, but keep sync)
            appContext?.let { ctx ->
                val callDurationMs = if (callStartTime > 0L) System.currentTimeMillis() - callStartTime else 0L
                NotificationHelper.showOngoingCallNotification(
                    context = ctx,
                    number = current.number,
                    name = current.name,
                    callDurationMs = callDurationMs,
                    isMuted = isMutedFlow.value,
                    isOnHold = isHeldFlow.value,
                    isRecording = isRecordingFlow.value
                )
            }
        }
    }

    fun mergeCalls() {
        val calls = activeCalls.toList()
        if (calls.size >= 2) {
            try {
                calls[0].conference(calls[1])
            } catch (e: Exception) { /* ignored */ }
        }
    }

    fun toggleHold() {
        val current = _callState.value
        isHeldFlow.value = !isHeldFlow.value
        try {
            if (isHeldFlow.value) telecomCall?.hold() else telecomCall?.unhold()
        } catch (e: Exception) { /* ignored */ }
        if (current is CallState.Active) {
            _callState.value = current.copy(isHeld = isHeldFlow.value)
            // Update notification to show new hold state
            appContext?.let { ctx ->
                val callDurationMs = if (callStartTime > 0L) System.currentTimeMillis() - callStartTime else 0L
                NotificationHelper.showOngoingCallNotification(
                    context = ctx,
                    number = current.number,
                    name = current.name,
                    callDurationMs = callDurationMs,
                    isMuted = isMutedFlow.value,
                    isOnHold = isHeldFlow.value,
                    isRecording = isRecordingFlow.value
                )
            }
        }
    }

    fun endCall(context: Context) {
        val current = _callState.value
        stopRingtone()
        SoundHelper.playCallEndTone()
        when (current) {
            is CallState.Active -> {
                try {
                    telecomCall?.disconnect()
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Ended")
            }
            is CallState.Incoming -> {
                try {
                    telecomCall?.reject(false, null)
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Rejected")
            }
            is CallState.Dialing -> {
                try {
                    telecomCall?.disconnect()
                } catch (e: Exception) { /* ignored */ }
                setDisconnected(current.number, current.name, "Cancelled")
            }
            else -> { /* nothing to do */ }
        }
        NotificationHelper.cancelOngoingCallNotification(context)
    }

    fun toggleRecording(context: Context) {
        val current = _callState.value
        val number = when (current) {
            is CallState.Active -> current.number
            is CallState.Dialing -> current.number
            else -> return
        }
        val name = when (current) {
            is CallState.Active -> current.name
            is CallState.Dialing -> current.name
            else -> null
        }
        isRecordingFlow.value = !isRecordingFlow.value
        if (isRecordingFlow.value) {
            recordingSecondsFlow.value = 0
            startRecordingTimer()
            RecordingService.startRecording(context, name ?: "Unknown", number)
        } else {
            stopRecordingTimer()
            val recordFile = RecordingService.stopRecording(context)
            if (recordFile != null) {
                scope.launch(Dispatchers.IO) {
                    val durationMs = recordingSecondsFlow.value * 1000
                    val container = com.example.diphone.DiPhoneApp.getContainer(context)
                    container.appDao.insertCallRecording(
                        CallRecording(
                            contactName = name,
                            phoneNumber = number,
                            filePath = recordFile.absolutePath,
                            duration = durationMs,
                            fileSize = recordFile.length(),
                            callType = 1
                        )
                    )
                }
            }
        }
        if (current is CallState.Active) {
            _callState.value = current.copy(
                isRecording = isRecordingFlow.value,
                recordingDuration = recordingSecondsFlow.value
            )
        }
    }

    // ─── Ringtone helpers ─────────────────────────────────────────────────────

    fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (e: Exception) { /* ignored */ }
        ringtone = null
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun resetCallAttributes() {
        isMutedFlow.value = false
        isSpeakerOnFlow.value = false
        isHeldFlow.value = false
        isRecordingFlow.value = false
        recordingSecondsFlow.value = 0L
        callStartTime = 0L
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _callState.value
                if (current is CallState.Active) {
                    _callState.value = current.copy(recordingDuration = recordingSecondsFlow.value)
                } else {
                    break
                }
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch {
            while (true) {
                delay(1000)
                recordingSecondsFlow.value++
                val current = _callState.value
                if (current is CallState.Active && isRecordingFlow.value) {
                    _callState.value = current.copy(recordingDuration = recordingSecondsFlow.value)
                } else {
                    break
                }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    private fun stopTimers() {
        callTimerJob?.cancel()
        callTimerJob = null
        stopRecordingTimer()
        resetCallAttributes()
    }

    fun getCallStartTime(): Long {
        return callStartTime
    }

    fun vibrate(context: Context, ms: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(ms)
                }
            }
        } catch (e: Exception) { /* ignored */ }
    }
}
