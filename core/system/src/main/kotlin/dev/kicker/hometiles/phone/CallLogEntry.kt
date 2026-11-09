package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.CallGrouping

enum class CallDirection { INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, OTHER }

data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
    val shown: CallerShown = CallerShown.NUMBER,
)

/** what the network let through about the caller. only [NUMBER] carries one to call back. */
enum class CallerShown { NUMBER, WITHHELD, UNKNOWN, PAYPHONE }

/**
 * reads the caller of a call log row.
 *
 * the log keeps the presentation apart from the number, and a withheld call can still come
 * with a number column that is not empty: older radios wrote their markers into it. read as
 * a number "-2" is the digit 2, shown as a caller and offered for a call back.
 */
object CallerIds {

    // CallLog.Calls.NUMBER_PRESENTATION: 1 allowed, 2 restricted, 3 unknown, 4 payphone,
    // 5 unavailable (api 31). null when the column is missing.
    fun shownOf(presentation: Int?, number: String): CallerShown {
        when (presentation) {
            2 -> return CallerShown.WITHHELD
            3, 5 -> return CallerShown.UNKNOWN
            4 -> return CallerShown.PAYPHONE
        }
        // the markers of CallerInfo, UNKNOWN_NUMBER, PRIVATE_NUMBER and PAYPHONE_NUMBER.
        return when (number.trim()) {
            "-1" -> CallerShown.UNKNOWN
            "-2" -> CallerShown.WITHHELD
            "-3" -> CallerShown.PAYPHONE
            else -> CallerShown.NUMBER
        }
    }

    /** empty unless there is a number to call back, so the row cannot dial a marker. */
    fun numberOf(shown: CallerShown, number: String): String =
        if (shown == CallerShown.NUMBER) number else ""

    /** a blank cached name is no name: it stood in the log as an empty row. */
    fun nameOf(name: String?): String? = name?.takeIf { it.isNotBlank() }
}

/** consecutive calls of one number, folded into a single row. */
data class CallGroup(
    val entries: List<CallEntry>,
) {
    val latest: CallEntry get() = entries.first()
    val count: Int get() = entries.size
    val number: String get() = latest.number
    val name: String? get() = entries.firstNotNullOfOrNull { it.name }
    val hasMissed: Boolean get() = entries.any { it.direction == CallDirection.MISSED }
}

/**
 * folds the call log.
 *
 * only *consecutive* calls of one number; otherwise the order in time shifts and last
 * called stops being true.
 */
object CallLogGrouping {

    fun group(entries: List<CallEntry>, mode: CallGrouping = CallGrouping.NUMBER): List<CallGroup> {
        val sorted = entries.sortedByDescending { it.timestamp }
        if (mode == CallGrouping.NONE) return sorted.map { CallGroup(listOf(it)) }
        val groups = mutableListOf<MutableList<CallEntry>>()
        sorted.forEach { entry ->
            val last = groups.lastOrNull()
            val cleaned = PhoneNumbers.clean(entry.number)
            // withheld numbers clean to "": grouping them would turn three anonymous
            // callers into one person who called three times.
            val fits = cleaned.isNotEmpty() && last?.firstOrNull()?.let {
                PhoneNumbers.clean(it.number) == cleaned &&
                    // a missed and an answered call are two events; folded, the older
                    // one's symbol would disappear.
                    (mode != CallGrouping.DIRECTION || it.direction == entry.direction)
            } ?: false
            if (fits) last!!.add(entry) else groups.add(mutableListOf(entry))
        }
        return groups.map { CallGroup(it) }
    }

    fun onlyMissed(groups: List<CallGroup>): List<CallGroup> = groups.filter { it.hasMissed }

    /** every id, so deleting a folded row deletes all the calls behind it. */
    fun idsOf(group: CallGroup): List<Long> = group.entries.map { it.id }

    fun idsOf(groups: List<CallGroup>): List<Long> = groups.flatMap(::idsOf)

    /**
     * the kinds left over by the config's hide list. unknown names are ignored: a backup
     * from a later version must not empty the call log.
     */
    fun allowedFrom(hidden: Set<String>): Set<CallDirection> =
        CallDirection.entries.filterNot { it.name in hidden }.toSet()

    fun visible(groups: List<CallGroup>, allowed: Set<CallDirection>): List<CallGroup> =
        if (allowed.isEmpty()) emptyList()
        else groups.filter { group -> group.entries.any { it.direction in allowed } }
}
