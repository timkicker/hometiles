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
     * Namenszusaetze wie "van" oder "von" zaehlen fuer die Reihenfolge nicht mit: "Anna van
     * Dijk" steht unter D, "Bernd von Ackeren" unter A. So haelt es das deutsche
     * Telefonbuch, und so sucht auch der Mensch, der sich an "Ackeren" erinnert und nicht
     * an das "von". Angezeigt wird der Name unveraendert - der Zusatz verschwindet nur aus
     * der Sortierung, und er haengt sich hinten an den Schluessel, damit "Dijk" und "van
     * Dijk" eine feste Reihenfolge behalten statt sich abzuwechseln.
     *
     * Am Emulator aufgefallen: "Bernd von Ackeren" stand unter V, "Emil de Vries" unter D -
     * jeder an der Stelle, an der man ihn nicht sucht. Der Test dazu behauptete in seinem
     * eigenen Kommentar "gehoert unter D, nicht unter V" und pruefte danach auf V; er hielt
     * den Fehler fest, statt ihn zu finden.
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
        // surnameStart zeigt auf den ersten Zusatz; der Nachname selbst faengt dahinter an.
        val zusaetze = parts.subList(surnameStart, parts.size - 1)
        val surname = parts.last()
        val rest = parts.subList(0, surnameStart)
        return TextSearch.normalize((listOf(surname) + rest + zusaetze).joinToString(" "))
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

    /**
     * Ob die Eingabe wie eine Telefonnummer aussieht.
     *
     * Gebraucht fuer den Fall, dass jemand eine Nummer eintippt, waehrend die Nummernsuche
     * abgeschaltet ist: dann steht "Kein Kontakt passt dazu" da, und das ist zwar wahr, aber
     * es verschweigt den Grund. Drei Ziffern als Untergrenze, damit ein Kontakt namens "X3"
     * nicht den Hinweis ausloest.
     */
    fun looksLikeNumber(query: String): Boolean {
        val ziffern = query.count { it.isDigit() }
        return ziffern >= 3 && query.none { it.isLetter() }
    }

    /** Ueber welche Felder gesucht wird. */
    fun searchText(contact: PhoneContact, includeNumbers: Boolean): String = buildString {
        append(contact.name)
        if (includeNumbers) {
            contact.numbers.forEach { append(' ').append(it.number) }
        }
    }

    private val PREFIXES = setOf("van", "von", "de", "del", "della", "der", "den", "di", "da", "du", "le", "la", "zu", "zur")
}
