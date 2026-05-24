package com.example.diphone.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_recordings")
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String? = null,
    val phoneNumber: String,
    val filePath: String,
    val duration: Long,       // duration in milliseconds
    val fileSize: Long,       // size in bytes
    val recordedAt: Long = System.currentTimeMillis(),
    val callType: Int         // 1 = INCOMING, 2 = OUTGOING
)
