package com.example.diphone.core.data.repository

import android.telephony.PhoneNumberUtils
import com.example.diphone.core.data.local.AppDao
import com.example.diphone.core.data.local.BlockedNumber
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class BlockedRepository(private val appDao: AppDao) {

    val blockedNumbers: Flow<List<BlockedNumber>> = appDao.getAllBlockedNumbers()

    fun normalizeNumber(number: String): String {
        val normalized = PhoneNumberUtils.normalizeNumber(number)
        return if (normalized.startsWith("0") && normalized.length > 5) {
            // Strip leading zero for local comparisons if needed, but usually full normalization is E.164
            normalized
        } else {
            normalized
        }
    }

    suspend fun isBlocked(number: String): Boolean {
        val clean = normalizeNumber(number)
        if (clean.isBlank()) return false
        // Search in local database
        if (appDao.isBlocked(clean)) return true
        
        // Also check if any suffix matches in case of prefix variants
        // (E.g. country codes like +91 9885107897 vs 9885107897)
        val allBlocked = appDao.getAllBlockedNumbers()
        // Wait, since getAllBlockedNumbers returns Flow, we can query directly
        // Let's do a direct comparison on suffix of last 10 digits
        if (clean.length >= 10) {
            val suffix = clean.takeLast(10)
            // Ideally we'd search in DAO, but let's query the specific E.164
            // and fallback
            return appDao.isBlocked(suffix)
        }
        return false
    }

    suspend fun blockNumber(number: String, name: String? = null, contactId: Long? = null, reason: String = "MANUAL") {
        val clean = normalizeNumber(number)
        if (clean.isNotBlank()) {
            appDao.insertBlockedNumber(
                BlockedNumber(
                    number = clean,
                    contactId = contactId,
                    contactName = name,
                    reason = reason
                )
            )
        }
    }

    suspend fun unblockNumber(number: String) {
        val clean = normalizeNumber(number)
        appDao.deleteBlockedNumberByNumber(clean)
        if (clean.length >= 10) {
            appDao.deleteBlockedNumberByNumber(clean.takeLast(10))
        }
    }

    suspend fun clearAll() {
        appDao.clearAllBlockedNumbers()
    }
}
