package org.biglau.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun load(limit: Int = 200): List<CallGroup> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        CallLogGrouping.group(readEntries(limit))
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
            // Kein "LIMIT" im Sortierparameter: der Anbieter lehnt das ab Android 11 ab
            // und wirft "Invalid token LIMIT" - was die App abschiesst. Begrenzt wird
            // beim Lesen.
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

    fun canWrite(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    /** Loescht die angegebenen Anrufe. Gibt zurueck, wie viele wirklich weg sind. */
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
