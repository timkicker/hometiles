package dev.kicker.hometiles.contacts

/** one row per number, as ContactsContract delivers it. */
data class ContactRow(
    val contactId: Long,
    val name: String,
    val number: String,
    val label: String?,
    val photoUri: String?,
    val starred: Boolean = false,
)

data class PhoneNumber(val number: String, val label: String?)

data class PhoneContact(
    val id: Long,
    val name: String,
    val photoUri: String?,
    val numbers: List<PhoneNumber>,
    val starred: Boolean = false,
) {
    /** nullable and not `first()`: a contact without a number would kill the launcher. */
    val primaryNumber: String? get() = numbers.firstOrNull()?.number
    val hasChoice: Boolean get() = numbers.size > 1
    val isCallable: Boolean get() = numbers.isNotEmpty()
}

/**
 * folds the rows into contacts.
 *
 * the provider gives one row per number and the same number more than once: from the
 * phone book and again from a synchronised account.
 */
object ContactMerge {

    /** digits and a leading plus: enough to spot two spellings of one number. */
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
                // first spelling wins, but a labelled row replaces an unlabelled one.
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
