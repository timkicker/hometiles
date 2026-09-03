package org.biglau.toggles

import org.biglau.phone.PhoneNumbers

/**
 * Die Nummernliste des Notrufs.
 *
 * Sie wird als eine Zeile eingegeben, weil das auf drei Zoll schneller geht als eine Liste
 * mit Plus-Knopf. Genau deshalb muss beim Einlesen streng sortiert werden: was hier
 * durchrutscht, ist im Ernstfall eine Nachricht, die nie ankommt.
 */
object SosNumbers {

    const val MAX = 5

    /**
     * Liest eine eingegebene Zeile. Getrennt wird an Komma, Semikolon und Zeilenumbruch -
     * der Nutzer soll nicht raten muessen, welches Zeichen gemeint ist.
     */
    fun parse(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && PhoneNumbers.isDialable(it) }
        .map { PhoneNumbers.clean(it) }
        .distinct()
        .take(MAX)

    /** Wieder als eine Zeile, so wie es im Eingabefeld steht. */
    fun format(numbers: List<String>): String = numbers.joinToString(", ")

    /** Was an der Eingabe unbrauchbar war - damit die Oberflaeche es sagen kann. */
    fun rejected(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && !PhoneNumbers.isDialable(it) }
}
