package dev.kicker.hometiles.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import dev.kicker.hometiles.data.CallGrouping
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun load(
        limit: Int = 200,
        mode: CallGrouping = CallGrouping.NUMBER,
    ): List<CallGroup> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        CallLogGrouping.group(readEntries(limit), mode)
    }

    private fun readEntries(limit: Int): List<CallEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )
        val entries = mutableListOf<CallEntry>()
        appContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            // no LIMIT in the sort argument: from android 11 the provider throws
            // "Invalid token LIMIT". the cap happens while reading.
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && entries.size < limit) {
                entries += CallEntry(
                    id = cursor.getLong(0),
                    number = cursor.getString(1).orEmpty(),
                    name = cursor.getString(2),
                    direction = directionOf(cursor.getInt(3)),
                    timestamp = cursor.getLong(4),
                    durationSeconds = cursor.getLong(5),
                )
            }
        }
        return entries
    }

    private fun directionOf(type: Int): CallDirection = when (type) {
        CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
        else -> CallDirection.OTHER
    }

    /**
     * missed calls nobody has seen yet.
     *
     * `NEW = 1` is the system's own answer, plus [since] so only calls younger than the
     * last look count; see [MissedCalls]. zero without the read permission: a guessed
     * number is worse than none.
     */
    suspend fun newMissedCount(since: Long = 0L): Int = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext 0
        runCatching {
            appContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                MissedCalls.selection(),
                MissedCalls.arguments(since),
                null,
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }

    /**
     * marks missed calls as seen, which is what a phone app does when the list is opened.
     *
     * without `WRITE_CALL_LOG` this silently returns 0 and the tile keeps blinking. the
     * caller ignores the result on purpose: could not mark as read helps nobody. the way
     * out sits a level up, in remembering the last look; `PLAN.md` 4.6.
     */
    suspend fun markMissedSeen(): Int = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext 0
        runCatching {
            appContext.contentResolver.update(
                CallLog.Calls.CONTENT_URI,
                android.content.ContentValues().apply { put(CallLog.Calls.NEW, 0) },
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = 1",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
            )
        }.getOrDefault(0)
    }

    fun canWrite(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    /** deletes the given calls; returns how many really went. */
    suspend fun delete(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        if (!canWrite() || ids.isEmpty()) return@withContext 0
        runCatching {
            val placeholders = ids.joinToString(",") { "?" }
            appContext.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} IN ($placeholders)",
                ids.map { it.toString() }.toTypedArray(),
            )
        }.getOrDefault(0)
    }

    suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext 0
        runCatching { appContext.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null) }
            .getOrDefault(0)
    }

    companion object {
        @Volatile
        private var instance: CallLogRepository? = null

        fun get(context: Context): CallLogRepository =
            instance ?: synchronized(this) {
                instance ?: CallLogRepository(context).also { instance = it }
            }
    }
}
