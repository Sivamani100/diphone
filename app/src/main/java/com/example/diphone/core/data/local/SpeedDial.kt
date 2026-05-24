package com.example.diphone.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial")
data class SpeedDial(
    @PrimaryKey val slot: Int, // 2-9 (1 is Voicemail, locked)
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val photoUri: String? = null
)
