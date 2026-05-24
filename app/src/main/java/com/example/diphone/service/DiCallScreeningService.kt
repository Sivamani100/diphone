package com.example.diphone.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.diphone.DiPhoneApp
import com.example.diphone.core.data.system.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""
        val response = CallResponse.Builder()
        
        if (number.isNotBlank()) {
            val container = DiPhoneApp.getContainer(applicationContext)
            val blockedRepository = container.blockedRepository
            val callLogRepository = container.callLogRepository
            
            CoroutineScope(Dispatchers.IO).launch {
                val isBlocked = blockedRepository.isBlocked(number)
                if (isBlocked) {
                    // Decline and auto-reject call
                    response.setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                    
                    respondToCall(callDetails, response.build())
                    
                    // Add entry to call logs as blocked
                    callLogRepository.addMockCall(
                        number = number,
                        name = "Blocked Caller",
                        type = 4, // BLOCKED
                        duration = 0,
                        spamTag = "Blocklisted number"
                    )

                    // Also post a missed call notification
                    NotificationHelper.showMissedCallNotification(
                        applicationContext,
                        number,
                        "Blocked Caller"
                    )
                } else {
                    response.setDisallowCall(false)
                    respondToCall(callDetails, response.build())
                }
            }
        } else {
            response.setDisallowCall(false)
            respondToCall(callDetails, response.build())
        }
    }
}
