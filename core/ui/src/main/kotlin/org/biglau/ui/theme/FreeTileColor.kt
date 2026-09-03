package org.biglau.ui.theme

import kotlin.math.abs

/**
 * Eine frei gewählte Kachelfarbe, die nicht unlesbar werden kann.
 *
 * `PLAN.md` 4.2 sagt „Kachelfarbe pro Kachel (Auto, Palette, **frei**)" zu, und 3.3 sagt
 * ebenso deutlich: Kontrast ist bei dieser App eine Funktion, kein Geschmack. Beides geht
 * nur zusammen, wenn der Mensch den **Farbton** wählt und die App die Helligkeit dazu
 * ausrechnet — die ist es nämlich, an der Lesbarkeit hängt, und sie ist nichts, wozu man
 * jemanden mit einem Schieberegler befragen sollte.
 *
 * Gespeichert wird deshalb der Farbton, nicht der fertige ARGB-Wert. Ein einmal
 * ausgerechneter Wert wäre an das Thema gebunden, in dem er entstand: dieselbe Farbe, die
 * im dunklen Thema sitzt, kann im hellen an der Schwelle für die Kachel gegen den
 * Hintergrund scheitern. Aus dem Farbton lässt sich zu jedem Thema die passende Fassung
 * neu bestimmen.
 */
object FreeTileColor {

    /**
     * Kennt dieses Thema überhaupt Kachelfarben?
     *
     * Im Kontrast-Thema tragen alle sechs Palettenplätze die Hintergrundfarbe — dort
     * stehen die Kacheln durch ihren Rand da, nicht durch ihre Füllung, und `PLAN.md` 3.3
     * sagt: „Im Kontrast-Theme werden Kachelfarben ignoriert — dort zählt nur
     * Schwarz/Gelb." Ein frei gewählter Ton muss deshalb genauso übergangen werden wie
     * eine Farbe aus der Palette.
     */
    fun themeUsesTileColours(tiles: List<Long>): Boolean = tiles.distinct().size > 1

    /** So viele Farbtöne stehen zur Wahl - 15° auseinander. */
    const val HUE_COUNT = 24

    /** Die wählbaren Farbtöne, gleichmäßig über den Kreis. */
    val hues: List<Float> = (0 until HUE_COUNT).map { it * (360f / HUE_COUNT) }

    /**
     * Das Gewicht, auf dem die Kacheln dieses Themas liegen.
     *
     * `PLAN.md` 3.3: „Alle sechs liegen absichtlich auf demselben Kontrastniveau - dadurch
     * wirkt keine Kachel gewichtiger als die andere, und die Unterscheidung laeuft rein
     * ueber den Farbton." Eine frei gewaehlte Farbe muss auf demselben Niveau liegen, sonst
     * sticht sie heraus, ohne dass das etwas bedeutet.
     */
    fun targetLuminance(tiles: List<Long>): Double = tiles.map(::relativeLuminance).average()

    /**
     * Die Fassung dieses Farbtons, die in diesem Thema beide Schwellen aus 3.3 hält —
     * Kachel gegen Hintergrund, Beschriftung auf der Kachel — und dabei so nah wie möglich
     * am Gewicht der Palette liegt ([targetLuminance]).
     *
     * Gesucht wird von kräftig nach blass: eine kräftige Farbe, die die Schwellen hält, ist
     * besser unterscheidbar als eine blasse, und unterscheidbar sein ist der ganze Zweck
     * einer Kachelfarbe.
     */
    fun forHue(hue: Float, background: Long, onTile: Long, targetLuminance: Double): Long {
        val ton = ((hue % 360f) + 360f) % 360f
        var bester: Long? = null
        var besteGuete = Double.MAX_VALUE
        for (schritt in 0..11) {
            val saettigung = 0.72f - schritt * 0.05f
            if (saettigung < 0.15f) break
            for (hundertstel in 10..80) {
                val farbe = hsl(ton, saettigung, hundertstel / 100f)
                val gegenGrund = contrastRatio(farbe, background)
                val schriftDarauf = contrastRatio(onTile, farbe)
                if (gegenGrund < Tokens.MIN_TILE_ON_BACKGROUND) continue
                if (schriftDarauf < Tokens.MIN_LABEL_ON_TILE) continue
                // Nicht die Schwellen maximieren, sondern das Gewicht der Palette treffen.
                // Maximiert wurde es zuerst - und im hellen Thema wanderte dann jeder Ton
                // gegen Schwarz: dort wachsen *beide* Verhaeltnisse, je dunkler die Kachel
                // ist. Am Bildschirmfoto gemessen: 14,5:1 statt der angepeilten 3,5, und
                // vierundzwanzig Toene, die alle gleich schwarz aussahen.
                val guete = abs(relativeLuminance(farbe) - targetLuminance)
                if (guete < besteGuete) {
                    besteGuete = guete
                    bester = farbe
                }
            }
            if (bester != null) return bester
        }
        // Kommt fuer die gebauten Themen nicht vor - FreeTileColorTest prueft jeden Ton in
        // jedem Thema. Bleibt trotzdem stehen: eine Kachel ohne Farbe waere schlimmer als
        // eine in der Farbe des Hintergrunds.
        return background
    }

    /** HSL nach ARGB, undurchsichtig. */
    private fun hsl(hue: Float, saturation: Float, lightness: Float): Long {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r, g, b) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun kanal(wert: Float): Long =
            ((wert + m).coerceIn(0f, 1f) * 255f).toInt().toLong()
        return 0xFF000000L or (kanal(r) shl 16) or (kanal(g) shl 8) or kanal(b)
    }
}

/** Eine Compose-Farbe als undurchsichtiger ARGB-Wert, wie ihn [contrastRatio] erwartet. */
fun androidx.compose.ui.graphics.Color.toArgbLong(): Long =
    0xFF000000L or
        ((red * 255f).toInt().toLong() shl 16) or
        ((green * 255f).toInt().toLong() shl 8) or
        (blue * 255f).toInt().toLong()
