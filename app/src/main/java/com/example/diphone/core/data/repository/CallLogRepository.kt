package com.example.diphone.core.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import com.example.diphone.core.data.local.AppDao
import com.example.diphone.core.data.local.CallRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int, // 1 = INCOMING, 2 = OUTGOING, 3 = MISSED, 4 = BLOCKED
    val date: Long,
    val duration: Long, // in seconds
    val photoUri: String? = null,
    val hasVoLTE: Boolean = true,
    val hasHD: Boolean = true,
    val isRecorded: Boolean = false,
    val spamTag: String? = null
)

class CallLogRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    private val _mockLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    // Cache for system call logs fetched on IO thread — prevents main thread crash
    private val _systemLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshSystemLogs()
        }
    }

    init {
        // Don't load mock logs initially — only use system call logs
        _mockLogs.value = emptyList()
        // Load system call log on IO thread in background
        scope.launch {
            _systemLogs.value = tryFetchSystemCallLogs()
        }
        try {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Exception) {}
    }

    fun getCallLogsStream(onlyMissed: Boolean = false): Flow<List<CallLogEntry>> {
        val recordingsFlow = appDao.getAllCallRecordings()

        return combine(recordingsFlow, _systemLogs, _mockLogs) { recordings, systemLogs, mocks ->
            // Only use mock logs if system logs are empty (no system call log data available)
            val baseList = if (systemLogs.isEmpty()) {
                mocks
            } else {
                // Use only system logs, ignore mocks when system data is available
                systemLogs
            }

            // Aggressive deduplication to prevent duplicate calls from appearing multiple times
            // Group by cleaned number and type, keep only one per group (the earliest one)
            val finalDeduplicated = mutableMapOf<Pair<String, Int>, CallLogEntry>()
            for (entry in baseList) {
                val cleanNumber = entry.number.filter { it.isDigit() }
                    .replace(Regex("^(1)?"), "") // Remove leading 1 if present
                    .takeLast(10) // Take last 10 digits for matching
                
                val key = Pair(cleanNumber, entry.type)
                
                // Keep the earliest (first) call for each number+type combination within a 5-second window
                if (!finalDeduplicated.containsKey(key)) {
                    finalDeduplicated[key] = entry
                } else {
                    val existing = finalDeduplicated[key]!!
                    // If this entry is earlier, replace it
                    if (entry.date < existing.date) {
                        finalDeduplicated[key] = entry
                    }
                    // Also check if they're within 5 seconds AND same duration - if so, keep first
                    else if (kotlin.math.abs(entry.date - existing.date) < 5000 &&
                             entry.duration == existing.duration &&
                             entry.date > existing.date) {
                        // Skip this duplicate
                    }
                }
            }

            // Check if any call matches a recording file
            val recordedPaths = recordings.map { it.phoneNumber.replace(" ", "") }

            // Convert deduplicated map back to sorted list
            var merged = finalDeduplicated.values.map { log ->
                val numberClean = log.number.replace(" ", "")
                val isRecorded = log.isRecorded || recordedPaths.contains(numberClean)
                log.copy(isRecorded = isRecorded)
            }

            if (onlyMissed) {
                merged = merged.filter { it.type == 3 || it.type == 4 }
            }

            merged.sortedByDescending { it.date }
        }
    }

    // Refresh system logs (call after permissions granted)
    fun refreshSystemLogs() {
        scope.launch {
            _systemLogs.value = tryFetchSystemCallLogs()
        }
    }

    suspend fun clearCallLog() = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
        } catch (e: Exception) {
            // Ignore — permission may be denied
        }
        _mockLogs.value = emptyList()
        _systemLogs.value = emptyList()
    }

    suspend fun deleteCallLogEntry(id: Long) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            // Ignore
        }
        _mockLogs.update { it.filterNot { log -> log.id == id } }
        _systemLogs.update { it.filterNot { log -> log.id == id } }
    }

    suspend fun addMockCall(
        number: String,
        name: String?,
        type: Int,
        duration: Long,
        isRecorded: Boolean = false,
        spamTag: String? = null
    ) = withContext(Dispatchers.IO) {
        val entry = CallLogEntry(
            id = System.currentTimeMillis(),
            number = number,
            name = name,
            type = type,
            date = System.currentTimeMillis(),
            duration = duration,
            isRecorded = isRecorded,
            spamTag = spamTag
        )
        _mockLogs.update { listOf(entry) + it }
    }

    private fun tryFetchSystemCallLogs(): List<CallLogEntry> {
        val list = mutableListOf<CallLogEntry>()
        val resolver = context.contentResolver

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_PHOTO_URI
        )

        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null, null,
                "${CallLog.Calls.DATE} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val photoCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)

                do {
                    val id = cursor.getLong(idCol)
                    val number = cursor.getString(numCol) ?: "Unknown"
                    val name = cursor.getString(nameCol)
                    val type = cursor.getInt(typeCol)
                    val date = cursor.getLong(dateCol)
                    val duration = cursor.getLong(durCol)
                    val photo = cursor.getString(photoCol)

                    list.add(
                        CallLogEntry(
                            id = id,
                            number = number,
                            name = name,
                            type = when (type) {
                                CallLog.Calls.INCOMING_TYPE -> 1
                                CallLog.Calls.OUTGOING_TYPE -> 2
                                CallLog.Calls.MISSED_TYPE -> 3
                                CallLog.Calls.BLOCKED_TYPE -> 4
                                else -> 1
                            },
                            date = date,
                            duration = duration,
                            photoUri = photo,
                            hasVoLTE = true,
                            hasHD = true
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            // Permission denied or log empty — will show mock data
        } finally {
            cursor?.close()
        }
        return list
    }

    private fun createMockLogs(): List<CallLogEntry> {
        // Return empty list - use real system call logs instead
        // Mock logs are only used as fallback when system logs unavailable
        return emptyList()
    }
}
