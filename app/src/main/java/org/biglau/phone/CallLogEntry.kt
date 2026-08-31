package org.biglau.phone

enum class CallDirection { INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, OTHER }

/** Eine Zeile aus der Anrufliste. */
data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
)

/** Mehrere aufeinanderfolgende Anrufe derselben Nummer, zu einer Zeile zusammengefasst. */
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
 * Fasst die Anrufliste zusammen.
 *
 * Ohne Gruppierung steht bei einem dreimal versuchten Anruf dreimal dieselbe Zeile - auf drei
 * Zoll ist die Liste dann nach zwei Kontakten voll. Zusammengefasst werden nur *aufeinander
 * folgende* Anrufe derselben Nummer; sonst verschoebe sich die zeitliche Reihenfolge, und
 * "zuletzt angerufen" waere keine verlaessliche Aussage mehr.
 */
object CallLogGrouping {

    fun group(entries: List<CallEntry>): List<CallGroup> {
        val sorted = entries.sortedByDescending { it.timestamp }
        val groups = mutableListOf<MutableList<CallEntry>>()
        sorted.forEach { entry ->
            val last = groups.lastOrNull()
            val cleaned = PhoneNumbers.clean(entry.number)
            // Unterdrueckte Nummern kommen ohne Ziffern an und bereinigen sich zu "".
            // Wuerde man danach gruppieren, erschienen drei verschiedene anonyme Anrufer
            // als ein einziger, dreimal anrufender Mensch.
            val sameNumber = cleaned.isNotEmpty() && last?.firstOrNull()?.let {
                PhoneNumbers.clean(it.number) == cleaned
            } ?: false
            if (sameNumber) last!!.add(entry) else groups.add(mutableListOf(entry))
        }
        return groups.map { CallGroup(it) }
    }

    fun onlyMissed(groups: List<CallGroup>): List<CallGroup> = groups.filter { it.hasMissed }

    /**
     * Alle Kennungen einer Gruppe. Wer eine zusammengefasste Zeile loescht, erwartet, dass
     * *alle* darin zusammengefassten Anrufe verschwinden - sonst taucht die Zeile nach dem
     * Neuladen mit einem Eintrag weniger wieder auf, und der Nutzer haelt das Loeschen fuer
     * kaputt.
     */
    fun idsOf(group: CallGroup): List<Long> = group.entries.map { it.id }

    fun idsOf(groups: List<CallGroup>): List<Long> = groups.flatMap(::idsOf)

    /** Sichtbar sind Gruppen, die mindestens einen Anruf der erlaubten Arten enthalten. */
    fun visible(groups: List<CallGroup>, allowed: Set<CallDirection>): List<CallGroup> =
        if (allowed.isEmpty()) emptyList()
        else groups.filter { group -> group.entries.any { it.direction in allowed } }
}
