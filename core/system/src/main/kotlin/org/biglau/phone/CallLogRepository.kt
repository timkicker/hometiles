package org.biglau.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import org.biglau.data.CallGrouping
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

    /**
     * Wie viele verpasste Anrufe noch niemand gesehen hat.
     *
     * `NEW = 1` ist die Auskunft des Systems darueber - dieselbe, aus der die Meldung
     * entsteht. Dazu [since]: nur Anrufe, die juenger sind als der letzte Blick in die
     * Liste. Siehe [MissedCalls], dort steht warum. Ohne Leseerlaubnis null: eine Zahl zu
     * raten waere schlimmer als keine.
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
     * Verpasste Anrufe als gesehen kennzeichnen - genau das tut eine Telefon-App, wenn
     * jemand die Liste oeffnet. Ohne diesen Schritt bliebe die Zahl auf der Kachel stehen,
     * obwohl der Nutzer sie gerade gelesen hat.
     *
     * **Und genau das passiert auf dem Telefon des Nutzers.** `WRITE_CALL_LOG` steht dort
     * auf `granted=false` (nachgesehen am 03.09.2026), also gibt diese Funktion still 0
     * zurueck und die Kachel blinkt weiter. Der Rueckgabewert wird beim Aufruf nicht
     * ausgewertet - er koennte es auch nicht sinnvoll, denn eine Meldung "konnte nicht als
     * gelesen markiert werden" hilft niemandem.
     *
     * Der Ausweg gehoert nicht hierher, sondern eine Ebene hoeher: BigLau kann sich selbst
     * merken, wann die Liste zuletzt offen war, und nur Neueres zaehlen. Dann braucht es
     * fuer ein Abzeichen gar kein Schreibrecht. Steht als offener Punkt in `PLAN.md` 4.6.
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
