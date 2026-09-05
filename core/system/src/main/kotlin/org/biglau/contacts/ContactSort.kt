package org.biglau.contacts

import org.biglau.search.TextSearch

enum class ContactOrder { FIRST_NAME, SURNAME }

/**
 * order of the contact list.
 *
 * the surname is derived from the display name rather than left to the device phone book,
 * which the original itself warns does not work on every phone.
 */
object ContactSort {

    /**
     * sort key: by surname the last part moves to the front, Anna Berger becomes berger anna.
     *
     * name prefixes like van or von do not count towards the order and are appended to the
     * key instead, so Dijk and van Dijk keep a fixed order rather than alternating.
     */
    fun sortKey(name: String, order: ContactOrder): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        if (order == ContactOrder.FIRST_NAME || parts.size == 1) {
            return TextSearch.normalize(parts.joinToString(" "))
        }

        var surnameStart = parts.size - 1
        while (surnameStart > 1 && parts[surnameStart - 1].lowercase() in PREFIXES) {
            surnameStart--
        }
        // surnameStart points at the first prefix; the surname itself follows behind it.
        val prefixes = parts.subList(surnameStart, parts.size - 1)
        val surname = parts.last()
        val rest = parts.subList(0, surnameStart)
        return TextSearch.normalize((listOf(surname) + rest + prefixes).joinToString(" "))
    }

    fun sorted(
        contacts: List<PhoneContact>,
        order: ContactOrder,
        favouritesFirst: Boolean = true,
    ): List<PhoneContact> = contacts.sortedWith(
        compareBy(
            { if (favouritesFirst && it.starred) 0 else 1 },
            { sortKey(it.name, order) },
        ),
    )

    /** a tile of its own, because the full list is a detour at 338 contacts. */
    fun favouritesOnly(
        contacts: List<PhoneContact>,
        order: ContactOrder,
    ): List<PhoneContact> = sorted(contacts.filter { it.starred }, order, favouritesFirst = false)

    /**
     * whether the input looks like a phone number.
     *
     * says why nothing matched while number search is off. three digits at least, so a
     * contact called X3 does not trigger the hint.
     */
    fun looksLikeNumber(query: String): Boolean {
        val digits = query.count { it.isDigit() }
        return digits >= 3 && query.none { it.isLetter() }
    }

    fun searchText(contact: PhoneContact, includeNumbers: Boolean): String = buildString {
        append(contact.name)
        if (includeNumbers) {
            contact.numbers.forEach { append(' ').append(it.number) }
        }
    }

    private val PREFIXES = setOf("van", "von", "de", "del", "della", "der", "den", "di", "da", "du", "le", "la", "zu", "zur")
}
