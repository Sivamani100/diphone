package com.example.diphone.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Blocked Numbers ---
    @Query("SELECT * FROM blocked_numbers ORDER BY blockedAt DESC")
    fun getAllBlockedNumbers(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers WHERE number = :number LIMIT 1")
    suspend fun getBlockedNumber(number: String): BlockedNumber?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumber): Long

    @Delete
    suspend fun deleteBlockedNumber(blockedNumber: BlockedNumber)

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun deleteBlockedNumberByNumber(number: String)

    @Query("DELETE FROM blocked_numbers")
    suspend fun clearAllBlockedNumbers()


    // --- Speed Dial ---
    @Query("SELECT * FROM speed_dial ORDER BY slot ASC")
    fun getAllSpeedDials(): Flow<List<SpeedDial>>

    @Query("SELECT * FROM speed_dial WHERE slot = :slot LIMIT 1")
    suspend fun getSpeedDial(slot: Int): SpeedDial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(speedDial: SpeedDial)

    @Query("DELETE FROM speed_dial WHERE slot = :slot")
    suspend fun deleteSpeedDial(slot: Int)


    // --- Call Recordings ---
    @Query("SELECT * FROM call_recordings ORDER BY recordedAt DESC")
    fun getAllCallRecordings(): Flow<List<CallRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecording(callRecording: CallRecording): Long

    @Delete
    suspend fun deleteCallRecording(callRecording: CallRecording)


    // --- Contact Augments (VIP, Ringtone, etc.) ---
    @Query("SELECT * FROM contact_augments")
    fun getAllContactAugments(): Flow<List<ContactAugment>>

    @Query("SELECT * FROM contact_augments WHERE contactId = :contactId LIMIT 1")
    suspend fun getContactAugment(contactId: Long): ContactAugment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactAugment(contactAugment: ContactAugment)

    @Query("SELECT * FROM contact_augments WHERE isVip = 1")
    fun getVipContactIds(): Flow<List<ContactAugment>>
}
