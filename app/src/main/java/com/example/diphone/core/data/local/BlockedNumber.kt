package com.example.diphone.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,           // normalized E.164 format
    val contactId: Long? = null,  // associated contact if any
    val contactName: String? = null,
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String = "MANUAL" // MANUAL, SPAM, PRIVATE
)
