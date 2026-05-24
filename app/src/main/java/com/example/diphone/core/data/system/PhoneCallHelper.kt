package com.example.diphone.core.data.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * PhoneCallHelper — Initiates REAL outgoing calls via the Android Telecom framework.
 *
 * When DiPhone is set as the default dialer:
 *   TelecomManager.placeCall directly initiates the cellular/PSTN call, bypassing
 *   external app choosers and launching our custom DiInCallService UI directly.
 *
 * For incoming calls, the OS binds DiInCallService automatically.
 * No simulation code exists here — everything is real.
 */
object PhoneCallHelper {

    /**
     * Place a real outgoing call.
     * Requires CALL_PHONE permission (requested at app startup in MainActivity).
     */
    fun placeCall(context: Context, number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "Enter a phone number first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check CALL_PHONE permission at runtime
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Phone call permission is required. Please grant it in App Settings.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val callUri = Uri.parse("tel:${Uri.encode(cleanNumber)}")
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            
            if (telecomManager != null) {
                // Direct call placement using TelecomManager, completely bypassing intent choosers
                val extras = Bundle()
                telecomManager.placeCall(callUri, extras)
            } else {
                // Fallback to standard ACTION_CALL intent if TelecomManager is unavailable
                val callIntent = Intent(Intent.ACTION_CALL, callUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
            }
        } catch (e: Exception) {
            // Ultimate fallback to ACTION_DIAL (always safe, prompts user to tap call button)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    "Unable to place call: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
