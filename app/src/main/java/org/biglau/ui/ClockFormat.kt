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

    /**
     * Hoehe der Kopfzeile in dp.
     *
     * Sie muss mit **beiden** Faktoren wachsen, die die Schrift darin groesser machen: der
     * globalen Textgroesse und der eigenen Groesse der Uhr. Gerechnet wurde nur mit der
     * zweiten - wer die Textgroesse auf 150 % stellte, bekam eine Kopfzeile, die ihre
     * Datumszeile mitten durchschnitt. Am Emulator gesehen, nachdem der Assistent mit
     * 150 % durchgelaufen war; der Kommentar an der Stelle beschrieb genau diesen Fehler
     * und die Rechnung deckte nur die Haelfte davon ab.
     */
    fun headerHeightDp(hasDate: Boolean, textScale: Float, clockScale: Float): Float =
        (if (hasDate) 78f else 54f) * textScale * scale(clockScale)

    /** Grob die Breite eines fetten serifenlosen Zeichens, gemessen an der Schriftgroesse. */
    private const val ZEICHENBREITE = 0.62f

    /**
     * Wie breit die Ladestandsanzeige rechts in der Kopfzeile wird.
     *
     * „100 %" plus Blitz plus Abstand. Sie waechst mit der Textgroesse mit, und genau das
     * macht sie zum Problem: bei 200 % nimmt sie so viel Platz, dass links nichts mehr
     * bleibt.
     */
    fun batteryWidthDp(textScale: Float): Float =
        "100 %".length * ZEICHENBREITE * 20f * textScale + 20f * textScale + 8f

    /**
     * Schriftgroesse der Uhr, damit sie **neben** dem Ladestand Platz hat.
     *
     * Bei 200 % Textgroesse liefen die beiden ineinander: die Uhr stand mit
     * `softWrap = false` da und zeichnete ueber den Ladestand hinweg, „9:37" und „100 %"
     * uebereinander. Am Emulator gesehen. Die Uhr darf deshalb kleiner werden, als die
     * Einstellung verlangt - eine Uhr, die man liest, ist mehr wert als eine, die die
     * gewuenschte Groesse hat und unter dem Ladestand verschwindet.
     */
    fun clockSizeSp(text: String, availableDp: Float, textScale: Float, clockScale: Float): Float {
        val gewuenscht = 26f * textScale * scale(clockScale)
        val passend = availableDp / (text.length.coerceAtLeast(1) * ZEICHENBREITE)
        return minOf(gewuenscht, passend).coerceAtLeast(14f)
    }
}
