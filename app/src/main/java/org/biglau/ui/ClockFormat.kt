package org.biglau.ui

import org.biglau.data.ClockDisplay

/**
 * Was die Uhr zeigt. PLAN.md 4.2: „aus / Uhrzeit / Uhrzeit+Datum / Uhrzeit+Datum+Wochentag".
 *
 * Der Wochentag ist die Stufe, die am meisten hilft und am meisten Platz kostet. Wer den
 * Tag nicht sicher weiss - und das ist haeufiger, als man denkt, wenn die Tage gleich
 * aussehen -, liest ihn hier ab. Wer ihn weiss, will die Zeile nicht.
 */
object ClockFormat {

    /**
     * Auf der Kachel gibt es kein „aus": eine Uhr-Kachel ohne Uhrzeit waere eine leere
     * Kachel, die aussieht, als sei etwas kaputt. „Aus" betrifft die Kopfzeile - dort
     * bleibt der Ladestand stehen, wenn die Uhr geht.
     */
    fun showsTime(display: ClockDisplay, onTile: Boolean): Boolean =
        onTile || display != ClockDisplay.OFF

    /** null heisst: keine Datumszeile. */
    fun datePattern(display: ClockDisplay, onTile: Boolean): String? = when (display) {
        ClockDisplay.OFF -> null
        ClockDisplay.TIME -> null
        // Auf der Kachel ist Platz fuer die langen Namen, in der Kopfzeile nicht.
        ClockDisplay.TIME_DATE -> if (onTile) "d. MMMM" else "d. MMM"
        ClockDisplay.TIME_DATE_WEEKDAY -> if (onTile) "EEEE, d. MMMM" else "EEE, d. MMM"
    }

    /** PLAN.md 4.2 „Groesse frei". Die Stufen, die die Kopfzeile noch traegt. */
    val SCALES = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    const val SCALE_MIN = 0.75f
    const val SCALE_MAX = 2.0f

    fun scale(value: Float): Float = value.coerceIn(SCALE_MIN, SCALE_MAX)
}
