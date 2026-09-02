package org.biglau.ui.theme

import org.biglau.data.ThemeName

/**
 * Der Hintergrund eines Screens. PLAN.md 4.1: „Themefarbe, Volltonfarbe, Bild … keins".
 *
 * Gebaut ist die Themefarbe und die Volltonfarbe. **Ein Bild bewusst nicht**, und zwar aus
 * demselben Grund, aus dem diese App ueberhaupt existiert: hinter einem Foto ist kein
 * Kontrast planbar - an jeder Stelle des Bildes anders. Die Kachelfarben sind gegen einen
 * bekannten Untergrund geprueft; ein Foto macht jede dieser Pruefungen ungueltig, und wer
 * schlecht sieht, verliert genau dort die Beschriftung.
 *
 * Die Volltonfarbe hat dagegen einen handfesten Nutzen: sie sagt beim Wischen auf einen
 * Blick, auf welchem Screen man ist - schneller, als der Name oben gelesen ist.
 */
object ScreenBackground {

    /**
     * Wie stark die Farbe den Themahintergrund einfaerbt.
     *
     * Zwoelf Prozent, und die Zahl ist nicht frei gewaehlt: der Themahintergrund hat
     * gegen die Kachelfarben nur 3,49 zu 1 Luft, und drei zu eins ist die Schwelle, unter
     * der man nicht mehr sieht, wo eine Kachel aufhoert. Bei 22 Prozent waren es 2,64 -
     * die Kacheln waeren im Grund verschwommen. Die Farbe soll den Screen kennzeichnen,
     * nicht die Schalttafel aufweichen.
     */
    private const val STRENGTH = 0.12f

    /**
     * Die Farbtoene. Nicht als fertige Farben, sondern als Einfaerbung des jeweiligen
     * Themahintergrunds: eine feste dunkle Liste sah im hellen Thema aus wie ein Fehler,
     * und die Kacheln haetten sich vom Grund nicht mehr abgehoben.
     */
    private val HUES: List<Long> = listOf(
        0xFF3B6FE0, // Blau
        0xFF7A4FD0, // Violett
        0xFF1E8A5A, // Gruen
        0xFFB03A46, // Rot
        0xFFB08A20, // Ocker
    )

    /**
     * Im Hochkontrast-Thema gibt es keine Wahl.
     *
     * Dort tragen Hintergrund und Kacheln dieselbe Farbe - die Kacheln stehen durch ihren
     * Rand da, nicht durch ihre Fuellung. Eine eigene Hintergrundfarbe wuerde genau die
     * eine Eigenschaft aufweichen, wegen der jemand dieses Thema waehlt.
     */
    fun offersChoices(theme: ThemeName): Boolean = theme != ThemeName.HIGH_CONTRAST

    fun choicesFor(theme: ThemeName, systemIsDark: Boolean): List<Long> {
        if (!offersChoices(theme)) return emptyList()
        val grund = paletteFor(theme, systemIsDark).background.value.toLong() shr 32
        return HUES.map { blend(grund, it, STRENGTH) }
    }

    /**
     * Die Tinte fuer alles, was direkt auf dem Hintergrund steht.
     *
     * Nicht die Tinte des Themas: die ist gegen die Themafarbe geprueft, und ein eigener
     * Hintergrund macht diese Pruefung ungueltig. Hier wird sie neu entschieden.
     */
    fun inkFor(background: Long): Long =
        if (contrastRatio(background, 0xFFFFFFFF) >= contrastRatio(background, 0xFF000000)) {
            0xFFFFFFFF
        } else {
            0xFF000000
        }

    private fun blend(base: Long, hue: Long, amount: Float): Long {
        fun kanal(shift: Int): Long {
            val a = (base shr shift) and 0xFF
            val b = (hue shr shift) and 0xFF
            return (a + (b - a) * amount).toLong().coerceIn(0, 255)
        }
        return (0xFFL shl 24) or (kanal(16) shl 16) or (kanal(8) shl 8) or kanal(0)
    }
}
