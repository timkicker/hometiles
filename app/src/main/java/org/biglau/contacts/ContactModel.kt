package org.biglau.contacts

/** Eine Zeile, wie sie aus ContactsContract kommt: pro Nummer eine. */
data class ContactRow(
    val contactId: Long,
    val name: String,
    val number: String,
    val label: String?,
    val photoUri: String?,
    val starred: Boolean = false,
)

data class PhoneNumber(val number: String, val label: String?)

/** Ein Kontakt mit allen seinen Nummern - so, wie ihn die Auswahl zeigt. */
data class PhoneContact(
    val id: Long,
    val name: String,
    val photoUri: String?,
    val numbers: List<PhoneNumber>,
    val starred: Boolean = false,
) {
    /**
     * Die erste Nummer, oder `null`.
     *
     * Bewusst nullbar und nicht `first()`: die Liste kommt heute aus einer Abfrage, die
     * ohne Nummer gar keine Zeile liefert - aber ein Kontakt ohne Nummer ist nichts
     * Unmoegliches, und ein `first()` auf einer leeren Liste wuerde den Launcher
     * abschiessen. Ein abgestuerzter Launcher ist ein schwarzes Telefon.
     */
    val primaryNumber: String? get() = numbers.firstOrNull()?.number
    val hasChoice: Boolean get() = numbers.size > 1
    val isCallable: Boolean get() = numbers.isNotEmpty()
}

/**
 * Fasst die Zeilen zu Kontakten zusammen. Die Anbieter-Datenbank liefert pro Nummer eine
 * Zeile und dieselbe Nummer gern mehrfach - einmal aus dem Telefonbuch, einmal aus einem
 * synchronisierten Konto. Ungefiltert stuende in der Auswahl dreimal dasselbe.
 */
object ContactMerge {

    /** Nur Ziffern und ein fuehrendes Plus - reicht, um Schreibweisen derselben Nummer zu erkennen. */
    fun normalizeNumber(number: String): String {
        val digits = number.filter { it.isDigit() }
        return if (number.trimStart().startsWith("+")) "+$digits" else digits
    }

    fun merge(rows: List<ContactRow>): List<PhoneContact> = rows
        .filter { it.name.isNotBlank() && normalizeNumber(it.number).any { c -> c.isDigit() } }
        .groupBy { it.contactId }
        .map { (id, group) ->
            val seen = LinkedHashMap<String, PhoneNumber>()
            group.forEach { row ->
                val key = normalizeNumber(row.number)
                // Die erste Schreibweise gewinnt, aber eine Zeile mit Bezeichnung
                // ersetzt eine zuvor gesehene ohne.
                val existing = seen[key]
                if (existing == null || (existing.label == null && row.label != null)) {
                    seen[key] = PhoneNumber(row.number.trim(), row.label)
                }
            }
            val first = group.first()
            PhoneContact(
                id = id,
                name = first.name.trim(),
                photoUri = group.firstNotNullOfOrNull { it.photoUri },
                numbers = seen.values.toList(),
                starred = group.any { it.starred },
            )
        }
        .sortedWith(compareBy({ !it.starred }, { it.name.lowercase() }))
}
