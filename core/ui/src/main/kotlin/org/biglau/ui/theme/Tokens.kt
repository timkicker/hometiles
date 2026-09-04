package org.biglau.ui.theme

import kotlin.math.pow

/**
 * Farbtokens als ARGB-Longs. Bewusst ohne Compose-Typen, damit die Kontrastpruefung
 * in einem gewoehnlichen JVM-Test laufen kann - PLAN.md 3.3 verlangt, dass die Werte
 * geprueft und nicht behauptet werden.
 */
object Tokens {

    // --- Dunkel, das Hauptthema ---
    const val DARK_BACKGROUND = 0xFF0A0A0AL
    const val DARK_ON_BACKGROUND = 0xFFFFFFFFL
    const val DARK_EMPTY_TILE = 0xFF161616L

    /**
     * Bei einer leeren Kachel ist die Umrandung der einzige Hinweis, dass dort ein Platz ist.
     * Sie muss deshalb die Flaechenschwelle erreichen - die stille Fuellung tut es nicht.
     */
    const val DARK_EMPTY_TILE_BORDER = 0xFF666666L
    const val DARK_ACCENT = 0xFF4C9AFFL

    /** Der dunkle Akzent ist hell - darauf gehoert dunkler Text, nicht weisser. */
    const val DARK_ON_ACCENT = 0xFF0A0A0AL
    const val DARK_DANGER = 0xFFFF5252L

    /** Auch das Rot ist hell genug, dass weisser Text darauf durchfaellt. */
    const val DARK_ON_DANGER = 0xFF0A0A0AL

    /**
     * Dasselbe Rot als **Schrift auf dem Hintergrund** - und deshalb ein anderer Wert.
     *
     * [DARK_DANGER] ist eine Flaechenfarbe: als Fuellung mit [DARK_ON_DANGER] darauf ist
     * sie in Ordnung. Als Text auf dem Hintergrund kam sie am 04.09.2026 auf **6,20:1** -
     * unter der eigenen Schwelle [MIN_TEXT_ON_BACKGROUND] von 7,0. Aufgefallen beim
     * Nachmessen einer neuen Warnung im Sicherungs-Import, und dann in zwoelf weiteren
     * Zeilen wiedergefunden: `ContrastTest` prueft die Schwelle seit jeher, aber nur fuer
     * [DARK_ON_BACKGROUND]. Fuer die Farbe, die **warnt**, hat sie nie jemand geprueft.
     */
    const val DARK_DANGER_TEXT = 0xFFFF8080L

    /** Sechs Kacheltoene auf gleichem Kontrastniveau, damit keine schwerer wiegt als die andere. */
    val DARK_TILES = listOf(
        0xFF2763CBL, // blau
        0xFF15773EL, // gruen
        0xFF9539CBL, // violett
        0xFF99561CL, // orange
        0xFF187086L, // tuerkis
        0xFFC22365L, // magenta
    )

    // --- Hell ---
    const val LIGHT_BACKGROUND = 0xFFFAFAFAL
    const val LIGHT_ON_BACKGROUND = 0xFF111111L
    const val LIGHT_EMPTY_TILE = 0xFFE8EAECL
    /**
     * Der Rahmen der leeren Kachel liegt an **zwei** Gruenden: aussen am Hintergrund,
     * innen an der Fuellung der Kachel. Bis zum 04.09.2026 wurde nur der aeussere geprueft;
     * innen kam `#8A8A8A` gegen `#E8EAEC` auf **2,86:1** und blieb damit unter der
     * Flaechenschwelle von 3,0. Am Emulator im Bildpunkt nachgesehen: der Rahmen sitzt
     * ohne Zwischenraum auf der Fuellung. `#7A7A7A` schafft 3,56 innen und 4,11 aussen.
     */
    const val LIGHT_EMPTY_TILE_BORDER = 0xFF7A7A7AL
    const val LIGHT_ACCENT = 0xFF1565C0L
    const val LIGHT_ON_ACCENT = 0xFFFFFFFFL
    const val LIGHT_DANGER = 0xFFC62828L

    /** Warnschrift auf hellem Grund - siehe [DARK_DANGER_TEXT]. */
    const val LIGHT_DANGER_TEXT = 0xFF8A1A1AL
    const val LIGHT_ON_DANGER = 0xFFFFFFFFL

    val LIGHT_TILES = listOf(
        0xFF1B4FA8L,
        0xFF106032L,
        0xFF7A2BA8L,
        0xFF7D4416L,
        0xFF12596BL,
        0xFF9E1B51L,
    )

    // --- Kontrast ---
    const val CONTRAST_BACKGROUND = 0xFF000000L
    const val CONTRAST_INK = 0xFFFFEB3BL
    const val CONTRAST_DANGER = 0xFFFF5252L

    /** Warnschrift im Kontrastthema - siehe [DARK_DANGER_TEXT]. */
    const val CONTRAST_DANGER_TEXT = 0xFFFF8080L
    const val CONTRAST_ON_ACCENT = 0xFF000000L
    const val CONTRAST_ON_DANGER = 0xFF000000L

    // --- Schwellen aus PLAN.md 3.3 ---
    /** Kachel gegen Hintergrund. */
    const val MIN_TILE_ON_BACKGROUND = 3.0

    /** Beschriftung auf einer Kachel - AAA fuer grossen Text. */
    const val MIN_LABEL_ON_TILE = 4.5

    /** Text ausserhalb einer Kachel - AAA fuer normalen Text. */
    const val MIN_TEXT_ON_BACKGROUND = 7.0
}

/** Relative Leuchtdichte nach WCAG 2.1. */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val raw = ((argb shr shift) and 0xFF).toDouble() / 255.0
        return if (raw <= 0.04045) raw / 12.92 else ((raw + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** Kontrastverhaeltnis nach WCAG 2.1, immer >= 1.0. */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}
