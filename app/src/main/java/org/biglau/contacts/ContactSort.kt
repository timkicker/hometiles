package org.biglau.contacts

import org.biglau.search.TextSearch

enum class ContactOrder { FIRST_NAME, SURNAME }

/**
 * Reihenfolge der Kontaktliste.
 *
 * Das Original ueberlaesst die Sortierung nach Nachnamen dem Telefonbuch des Geraets und
 * warnt selbst, dass sie "nicht auf allen Telefonen funktioniert". Wir rechnen sie deshalb
 * selbst aus dem Anzeigenamen - das ist zwar eine Heuristik, aber eine, die man pruefen
 * und erklaeren kann.
 */
object ContactSort {

    /**
     * Sortierschluessel. Bei Nachnamen-Sortierung wandert der letzte Namensteil nach vorn,
     * der Rest folgt - "Anna Berger" wird zu "berger anna".
     *
     * Namenszusaetze wie "van" oder "von" bleiben beim Nachnamen: "Anna van Dijk" gehoert
     * unter D wie "van Dijk", nicht unter V.
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
        val surname = parts.subList(surnameStart, parts.size).joinToString(" ")
        val rest = parts.subList(0, surnameStart).joinToString(" ")
        return TextSearch.normalize(if (rest.isEmpty()) surname else "$surname $rest")
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

    /**
     * Nur die Favoriten, in derselben Reihenfolge wie sonst.
     *
     * Eine eigene Kachel dafuer, weil die volle Liste bei 338 Kontakten selbst mit Suche
     * ein Umweg ist - und weil die drei bis fuenf Menschen, die man taeglich anruft,
     * genau die sind, fuer die diese App gemacht ist.
     */
    fun favouritesOnly(
        contacts: List<PhoneContact>,
        order: ContactOrder,
    ): List<PhoneContact> = sorted(contacts.filter { it.starred }, order, favouritesFirst = false)

    /** Ueber welche Felder gesucht wird. */
    fun searchText(contact: PhoneContact, includeNumbers: Boolean): String = buildString {
        append(contact.name)
        if (includeNumbers) {
            contact.numbers.forEach { append(' ').append(it.number) }
        }
    }

    private val PREFIXES = setOf("van", "von", "de", "del", "della", "der", "den", "di", "da", "du", "le", "la", "zu", "zur")
}
