package org.biglau.phone

import android.provider.CallLog

/**
 * which missed calls nobody has seen yet. the condition lives here rather than as a string in
 * the query, so it can be checked without a phone.
 *
 * **both conditions are needed.** `NEW = 1` is the system's word: going through the list in
 * the system phone app clears it here too. the timestamp is ours, because biglau may not
 * write to the call log, and without it the number would stand for ever.
 */
object MissedCalls {

    /** the `WHERE` clause. */
    fun selection(): String =
        "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = 1 AND ${CallLog.Calls.DATE} > ?"

    /** its arguments, in the same order. */
    fun arguments(since: Long): Array<String> =
        arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.coerceAtLeast(0L).toString())

    /**
     * **not "now" but the newest call in the list.** time passes between reading and saving,
     * and a call arriving exactly in between would be ticked off as seen by nobody. an empty
     * list leaves the old timestamp: there was nothing to see.
     */
    fun seenUpTo(previous: Long, entries: List<CallEntry>): Long =
        maxOf(previous, entries.maxOfOrNull { it.timestamp } ?: 0L)
}
