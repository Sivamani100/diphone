package com.example.diphone.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RecordingService"
        
        @Volatile private var mediaRecorder: MediaRecorder? = null
        @Volatile private var currentFile: File? = null
        @Volatile private var isCurrentlyRecording = false

        fun startRecording(context: Context, contactName: String, number: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                putExtra("NAME", contactName)
                putExtra("NUMBER", number)
                action = "START"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "startRecording called for $contactName")
        }

        fun stopRecording(context: Context): File? {
            val file = currentFile
            Log.d(TAG, "stopRecording called, file path: ${file?.absolutePath}")
            val intent = Intent(context, RecordingService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
            return file
        }

        fun isRecording(): Boolean = isCurrentlyRecording
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")
        
        if (action == "START") {
            val name = intent?.getStringExtra("NAME") ?: "Unknown"
            val number = intent?.getStringExtra("NUMBER") ?: "Unknown"
            Log.d(TAG, "Starting recording for: $name ($number)")
            startForegroundNotification(name)
            initRecorder(name, number)
        } else if (action == "STOP") {
            Log.d(TAG, "Stopping recorder")
            stopRecorder()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification(name: String) {
        val channelId = "call_recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Recording Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Recording call")
            .setContentText("Recording audio for call with $name")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .build()

        try {
            startForeground(99, notification)
            Log.d(TAG, "Foreground notification started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground: ${e.message}", e)
        }
    }

    private fun initRecorder(name: String, number: String) {
        try {
            Log.d(TAG, "Initializing recorder...")
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanName = name.replace(" ", "_").replace("/", "_")
            val cleanNumber = number.replace(" ", "").replace("+", "")
            val fileName = "${cleanName}_${cleanNumber}_$timeStamp.m4a"

            // Use app-isolated external files dir — no WRITE_EXTERNAL_STORAGE needed
            val targetDir = getExternalFilesDir("Recordings")
                ?: File(filesDir, "Recordings")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
                Log.d(TAG, "Created recordings directory: ${targetDir.absolutePath}")
            }

            val destFile = File(targetDir, fileName)
            currentFile = destFile
            Log.d(TAG, "Recording file path: ${destFile.absolutePath}")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(applicationContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                // VOICE_COMMUNICATION applies echo cancellation & noise suppression for call audio
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(destFile.absolutePath)
                prepare()
                start()
                Log.d(TAG, "MediaRecorder started successfully")
            }
            mediaRecorder = recorder
            isCurrentlyRecording = true
            Log.d(TAG, "Recording initialized successfully")
        } catch (voiceCommunicationError: Exception) {
            Log.w(TAG, "VOICE_COMMUNICATION source failed: ${voiceCommunicationError.message}, trying MIC fallback")
            tryMicFallback(name, number)
        }
    }

    private fun tryMicFallback(name: String, number: String) {
        try {
            Log.d(TAG, "Attempting MIC audio source fallback...")
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanName = name.replace(" ", "_").replace("/", "_")
            val cleanNumber = number.replace(" ", "").replace("+", "")
            val fileName = "${cleanName}_${cleanNumber}_fallback_$timeStamp.m4a"
            
            val dir = getExternalFilesDir("Recordings") ?: File(filesDir, "Recordings").apply { mkdirs() }
            val destFile = File(dir, fileName)
            currentFile = destFile
            
            Log.d(TAG, "Fallback recording file: ${destFile.absolutePath}")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(applicationContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(destFile.absolutePath)
                prepare()
                start()
                Log.d(TAG, "MIC fallback recorder started successfully")
            }
            mediaRecorder = recorder
            isCurrentlyRecording = true
            Log.d(TAG, "MIC fallback recording initialized")
        } catch (micError: Exception) {
            Log.e(TAG, "Both VOICE_COMMUNICATION and MIC sources failed: ${micError.message}", micError)
            isCurrentlyRecording = false
        }
    }

    private fun stopRecorder() {
        try {
            if (mediaRecorder != null) {
                Log.d(TAG, "Stopping MediaRecorder...")
                mediaRecorder?.stop()
                mediaRecorder?.release()
                Log.d(TAG, "MediaRecorder stopped and released")
                
                // Log file info
                currentFile?.let { file ->
                    if (file.exists()) {
                        val sizeInKB = file.length() / 1024
                        Log.d(TAG, "Recording saved: ${file.name} (${sizeInKB}KB) at ${file.absolutePath}")
                    } else {
                        Log.w(TAG, "Recording file does not exist after stopping: ${file.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}", e)
        } finally {
            mediaRecorder = null
            isCurrentlyRecording = false
        }
    }
}
