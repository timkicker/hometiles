package org.biglau.info

/**
 * Wann die Uhr das naechste Mal neu gezeichnet werden muss.
 *
 * Sekuendlich aufzuwachen kostet auf einem 2000-mAh-Akku spuerbar Strom, und die Kachel
 * zeigt ohnehin keine Sekunden. Gerechnet wird deshalb bis zur naechsten vollen Minute -
 * nicht schlicht 60 Sekunden, sonst laeuft die Anzeige langsam aus dem Takt.
 */
object ClockTick {

    private const val MINUTE = 60_000L

    fun millisUntilNextMinute(epochMillis: Long): Long {
        val intoMinute = ((epochMillis % MINUTE) + MINUTE) % MINUTE
        val remaining = MINUTE - intoMinute
        return if (remaining <= 0L) MINUTE else remaining
    }
}
