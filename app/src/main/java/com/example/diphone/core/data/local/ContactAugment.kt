package com.example.diphone.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_augments")
data class ContactAugment(
    @PrimaryKey val contactId: Long,
    val isVip: Boolean = false,
    val customRingtone: String? = null,
    val isBlocked: Boolean = false,
    val routeToVoicemail: Boolean = false
)
