package org.biglau.phone

import org.biglau.data.CallGrouping

enum class CallDirection { INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, OTHER }

data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
)

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
