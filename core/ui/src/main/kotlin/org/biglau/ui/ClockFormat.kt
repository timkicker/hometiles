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

    /**
     * Welche **Bestandteile** das Datum hat - nicht, wie sie angeordnet sind.
     *
     * Hier stand vorher ein fertiges deutsches Muster („EEEE, d. MMMM"). Mit einer anderen
     * Sprache kam damit Unsinn heraus: am Jelly 2, das auf Englisch steht, hiess der
     * Wochentag „Wednesday, 2. September" - englischer Name, deutscher Punkt, deutsche
     * Reihenfolge. Ein Datum, das man zweimal lesen muss, ist auf einer Uhr das Gegenteil
     * dessen, wofuer sie da ist.
     *
     * Ein Skelett sagt nur „Wochentag, Tag, Monat"; die Anordnung holt sich die Anzeige
     * ueber `DateFormat.getBestDateTimePattern` aus der Sprache. Null heisst: keine
     * Datumszeile.
     */
    fun dateSkeleton(display: ClockDisplay, onTile: Boolean): String? =
        dateSkeletons(display, onTile).firstOrNull()

    /**
     * Die Stufen vom langen zum kurzen Datum - erst kuerzen, dann umbrechen.
     *
     * „Auf der Kachel ist Platz fuer die langen Namen" stand hier als Behauptung, und am
     * Geraet stimmte sie nicht: auf einer 1x1-Kachel brach „Wednesday, September 2" um, und
     * in der zweiten Zeile stand die **2 allein**. Eine Zahl ohne ihren Monat ist kein
     * Datum mehr, sondern ein Rest.
     *
     * Die Kachel misst deshalb (siehe `ClockContent`): passt der lange Name nicht in eine
     * Zeile, nimmt sie den kurzen. Die Kopfzeile ist von vornherein schmal und faengt
     * gleich beim kurzen an.
     */
    fun dateSkeletons(display: ClockDisplay, onTile: Boolean): List<String> = when (display) {
        ClockDisplay.OFF -> emptyList()
        ClockDisplay.TIME -> emptyList()
        ClockDisplay.TIME_DATE -> if (onTile) listOf("dMMMM", "dMMM") else listOf("dMMM")
        ClockDisplay.TIME_DATE_WEEKDAY ->
            if (onTile) listOf("EEEEdMMMM", "EEEdMMM", "dMMM") else listOf("EEEdMMM")
    }

    /**
     * Das laengste Datum, das dieses Muster im Lauf eines Jahres ergibt.
     *
     * Gemessen wird nicht mit dem heutigen Datum: sonst passte die Kachel am Mittwoch und
     * braeche am Donnerstag um, und der Nutzer saehe einen Fehler, der von der Woche
     * abhaengt. Durchgegangen werden alle zwoelf Monate an sieben aufeinanderfolgenden
     * Tagen - das deckt jeden Wochentagsnamen und jeden Monatsnamen ab, und die Tageszahl
     * ist dabei immer zweistellig.
     *
     * **Laenge in Zeichen ist ein Naeherungswert**, nicht die Breite in Pixeln; ein „W"
     * ist breiter als ein „i". Fuer die Frage „lange oder kurze Namen" reicht das, und die
     * Alternative waere, vierundachtzig Zeichenketten zu vermessen, bei jeder Minute neu.
     */
    fun longestDate(format: java.text.DateFormat): String {
        val kalender = java.util.Calendar.getInstance()
        var laengste = ""
        for (monat in 0..11) {
            for (tag in 22..28) {
                kalender.set(2024, monat, tag)
                val text = format.format(kalender.time)
                if (text.length > laengste.length) laengste = text
            }
        }
        return laengste
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
