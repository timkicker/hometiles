package org.biglau.phone

/**
 * Gesperrte Nummern (`PLAN.md` 4.6).
 *
 * Verglichen wird über die **letzten Ziffern**, nicht Zeichen für Zeichen: dieselbe Person
 * erscheint als „+43 664 1234567", „0043 664 1234567" und „0664 1234567", je nachdem, wer
 * die Nummer eingetragen hat und welches Netz sie mitschickt. Ein Vergleich auf Gleichheit
 * würde in zwei von drei Fällen danebengehen — und eine Sperre, die manchmal nicht greift,
 * ist schlimmer als keine, weil man sich auf sie verlässt.
 *
 * `SUFFIX_DIGITS` ist die Länge einer österreichischen Rufnummer ohne Vorwahl. Kürzer zu
 * vergleichen hieße, fremde Nummern mitzusperren.
 */
object CallBlocking {

    const val SUFFIX_DIGITS = 7

    /** Die Ziffern, auf die es ankommt - ohne Plus, Nullen und Trennzeichen davor. */
    fun key(number: String): String =
        PhoneNumbers.clean(number).filter { it.isDigit() }.takeLast(SUFFIX_DIGITS)

    /**
     * Ist diese Nummer gesperrt?
     *
     * Notrufnummern **nie** — `PLAN.md` 4.6 sagt, sie gehen immer durch, und das gilt in
     * beide Richtungen: eine versehentlich gesperrte 112 wäre der teuerste Fehler, den
     * diese App machen kann. Eine unterdrückte Nummer (ohne Ziffern) ist ebenfalls nicht
     * sperrbar, sonst träfe eine Sperre jeden anonymen Anrufer auf einmal.
     */
    fun isBlocked(number: String, blocked: Collection<String>): Boolean {
        if (PhoneNumbers.looksLikeEmergency(number)) return false
        val schluessel = key(number)
        if (schluessel.length < SUFFIX_DIGITS) return false
        return blocked.any { key(it) == schluessel }
    }

    /**
     * Die Frage, die [CallScreening] stellt: abweisen, bevor es klingelt?
     *
     * Nur **eingehende** Anrufe. Android fragt den Dienst auch bei abgehenden, und dort
     * hiesse ein Ja: der Nutzer waehlt eine Nummer, die er selbst gesperrt hat, und der
     * Anruf kommt nicht zustande, ohne dass ihm jemand sagt warum. Eine Sperre ist gegen
     * andere gerichtet, nicht gegen die eigene Hand.
     */
    fun blocksIncoming(number: String, incoming: Boolean, blocked: Collection<String>): Boolean =
        incoming && isBlocked(number, blocked)

    /** Zerlegt die Eingabezeile; leere und zu kurze Einträge fallen weg. */
    fun parse(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { key(it).length >= SUFFIX_DIGITS }
        .distinctBy(::key)

    /** Was beim Einlesen aussortiert wurde - damit es nicht still verschwindet. */
    fun rejected(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && key(it).length < SUFFIX_DIGITS }

    fun format(numbers: List<String>): String = numbers.joinToString(", ")
}
