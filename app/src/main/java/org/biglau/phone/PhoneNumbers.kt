package org.biglau.phone

/**
 * Umgang mit Rufnummern - insbesondere mit Notrufnummern.
 *
 * Warum das hier und nicht nur ueber die Android-API laeuft: manche Geraete lassen einen
 * Notruf aus einer Fremd-App gar nicht zu, andere leiten ihn still um. Das Original hat
 * dafuer eine Einstellung. Wir wollen keine Einstellung, sondern eine Regel: eine Nummer,
 * die *irgendwie* nach Notruf aussieht, waehlt BigLau nie selbst, sondern uebergibt sie an
 * den System-Dialer. Lieber ein Tastendruck mehr als ein verschluckter Notruf.
 *
 * Die Liste ergaenzt die Plattformpruefung, sie ersetzt sie nicht.
 */
object PhoneNumbers {

    /** Notrufnummern, die ohne Netz und ohne SIM gelten sollen. Bewusst grosszuegig. */
    val WELL_KNOWN_EMERGENCY = setOf(
        "112", "911", "999", "000", "110", "118", "119", "115", "122", "133", "144", "911",
        "08", "999", "112",
    )

    /** Nur Ziffern, Plus und die Waehlzeichen, die das Netz kennt. */
    fun clean(number: String): String =
        number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    /**
     * Sieht die Nummer nach Notruf aus? Bewusst grosszuegig: ein falsch positiver Treffer
     * kostet einen Tastendruck im System-Dialer, ein falsch negativer kann einen Notruf kosten.
     */
    fun looksLikeEmergency(number: String, platformSaysYes: Boolean = false): Boolean {
        if (platformSaysYes) return true
        val digits = clean(number).removePrefix("+")
        if (digits.isEmpty()) return false
        return digits in WELL_KNOWN_EMERGENCY
    }

    /** Sinnvoll waehlbar? Leere und reine Zeichenfolgen ohne Ziffer sind es nicht. */
    fun isDialable(number: String): Boolean = clean(number).any { it.isDigit() }

    /**
     * Gruppiert lange Nummern in Bloecke, damit sie auf drei Zoll lesbar bleiben.
     * Keine Laendervorwahlen-Logik - die waere ohne Bibliothek geraten.
     */
    fun forDisplay(number: String): String {
        val cleaned = clean(number)
        if (cleaned.length <= 6) return cleaned
        val plus = cleaned.startsWith("+")
        val body = if (plus) cleaned.drop(1) else cleaned
        val grouped = body.chunked(3).joinToString(" ")
        return if (plus) "+$grouped" else grouped
    }
}
