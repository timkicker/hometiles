package org.biglau.ui

import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 3.3 legt die Kontrastschwellen als Funktion fest, nicht als Geschmack.
 * Wer eine Farbe aendert, faellt hier durch statt es erst auf dem Geraet zu merken.
 */
class ContrastTest {

    @Test
    fun `bekannte Verhaeltnisse stimmen mit der WCAG-Formel ueberein`() {
        // Schwarz auf Weiss ist per Definition 21:1.
        assertEquals(21.0, contrastRatio(0xFF000000L, 0xFFFFFFFFL), 0.01)
        // Gleiche Farbe ergibt 1:1.
        assertEquals(1.0, contrastRatio(0xFF2763CBL, 0xFF2763CBL), 0.0001)
        // Die Reihenfolge der Argumente darf nichts aendern.
        assertEquals(
            contrastRatio(0xFF2763CBL, Tokens.DARK_BACKGROUND),
            contrastRatio(Tokens.DARK_BACKGROUND, 0xFF2763CBL),
            0.0001,
        )
    }

    @Test
    fun `jede dunkle Kachel hebt sich vom Hintergrund ab`() {
        Tokens.DARK_TILES.forEach { tile ->
            val ratio = contrastRatio(tile, Tokens.DARK_BACKGROUND)
            assertTrue(
                "Kachel ${tile.toHex()} erreicht nur %.2f:1 gegen den Hintergrund".format(ratio),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
    }

    @Test
    fun `weisse Beschriftung ist auf jeder dunklen Kachel lesbar`() {
        Tokens.DARK_TILES.forEach { tile ->
            val ratio = contrastRatio(0xFFFFFFFFL, tile)
            assertTrue(
                "Weiss auf ${tile.toHex()} erreicht nur %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `die Kacheln liegen alle auf demselben Kontrastniveau`() {
        // Sonst wirkt eine Kachel schwerer als die andere und die Unterscheidung
        // laeuft nicht mehr rein ueber den Farbton.
        val ratios = Tokens.DARK_TILES.map { contrastRatio(it, Tokens.DARK_BACKGROUND) }
        val spread = ratios.max() - ratios.min()
        assertTrue("Kontrastspanne betraegt %.2f, erlaubt sind 0,3".format(spread), spread <= 0.3)
    }

    @Test
    fun `Text ausserhalb der Kacheln erreicht die strengere Schwelle`() {
        assertTrue(
            contrastRatio(Tokens.DARK_ON_BACKGROUND, Tokens.DARK_BACKGROUND) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
        assertTrue(
            contrastRatio(Tokens.DARK_ON_BACKGROUND, Tokens.DARK_EMPTY_TILE) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
        assertTrue(
            contrastRatio(Tokens.LIGHT_ON_BACKGROUND, Tokens.LIGHT_BACKGROUND) >= Tokens.MIN_TEXT_ON_BACKGROUND,
        )
    }

    /**
     * Die Warnfarbe wird an fuenfzehn Stellen als **Schrift** auf dem Hintergrund
     * gezeichnet - dort, wo etwas schiefgehen kann. Bis zum 04.09.2026 hat das niemand
     * geprueft: die Regel darueber sah nur `ON_BACKGROUND` an, und `DANGER` kam in keinem
     * einzigen Test gegen einen Hintergrund vor.
     *
     * Nachgemessen war das alte Rot bei **6,20:1** (dunkel), **5,39:1** (hell) und
     * **6,58:1** (Kontrast) - dreimal unter der eigenen Schwelle von 7,0. Deshalb gibt es
     * jetzt einen eigenen Schriftton; die Flaechenfarbe bleibt, wie sie war.
     */
    @Test
    fun `die Warnschrift erreicht die strengere Schwelle in jedem Thema`() {
        listOf(
            Triple("dunkel", Tokens.DARK_DANGER_TEXT, Tokens.DARK_BACKGROUND),
            Triple("hell", Tokens.LIGHT_DANGER_TEXT, Tokens.LIGHT_BACKGROUND),
            Triple("Kontrast", Tokens.CONTRAST_DANGER_TEXT, Tokens.CONTRAST_BACKGROUND),
            // Auch auf der leeren Kachel: dort steht sie im Kachel-Editor.
            Triple("dunkel, leere Kachel", Tokens.DARK_DANGER_TEXT, Tokens.DARK_EMPTY_TILE),
            Triple("hell, leere Kachel", Tokens.LIGHT_DANGER_TEXT, Tokens.LIGHT_EMPTY_TILE),
        ).forEach { (name, schrift, grund) ->
            val ratio = contrastRatio(schrift, grund)
            assertTrue(
                "Warnschrift ($name) erreicht nur %.2f:1, verlangt sind %.1f"
                    .format(ratio, Tokens.MIN_TEXT_ON_BACKGROUND),
                ratio >= Tokens.MIN_TEXT_ON_BACKGROUND,
            )
        }
    }

    /**
     * Die Flaechenfarbe bleibt eine Flaechenfarbe: sie muss sich vom Hintergrund abheben
     * und ihre eigene Beschriftung tragen. Beides galt vorher und gilt weiter - der neue
     * Schriftton ersetzt sie nicht, er steht daneben.
     */
    @Test
    fun `die Warnflaeche bleibt eine Flaeche`() {
        listOf(
            Triple("dunkel", Tokens.DARK_DANGER, Tokens.DARK_BACKGROUND),
            Triple("hell", Tokens.LIGHT_DANGER, Tokens.LIGHT_BACKGROUND),
        ).forEach { (name, flaeche, grund) ->
            val ratio = contrastRatio(flaeche, grund)
            assertTrue(
                "Warnflaeche ($name) hebt sich nur %.2f:1 ab".format(ratio),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
        listOf(
            Triple("dunkel", Tokens.DARK_ON_DANGER, Tokens.DARK_DANGER),
            Triple("hell", Tokens.LIGHT_ON_DANGER, Tokens.LIGHT_DANGER),
            Triple("Kontrast", Tokens.CONTRAST_ON_DANGER, Tokens.CONTRAST_DANGER),
        ).forEach { (name, schrift, flaeche) ->
            val ratio = contrastRatio(schrift, flaeche)
            assertTrue(
                "Schrift auf der Warnflaeche ($name) erreicht nur %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `das Kontrastthema bleibt bei Schwarz und Gelb`() {
        val ratio = contrastRatio(Tokens.CONTRAST_INK, Tokens.CONTRAST_BACKGROUND)
        assertTrue("Gelb auf Schwarz erreicht nur %.2f:1".format(ratio), ratio >= 15.0)
    }

    @Test
    fun `helle Kacheln tragen ebenfalls weisse Beschriftung`() {
        Tokens.LIGHT_TILES.forEach { tile ->
            assertTrue(
                "Weiss auf ${tile.toHex()} ist zu schwach",
                contrastRatio(0xFFFFFFFFL, tile) >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `Text auf der Akzentflaeche ist lesbar`() {
        // Weiss auf dem hellen Akzent des dunklen Themas erreichte nur 2,85:1 -
        // deshalb gibt es ein eigenes Token dafuer statt einer Annahme.
        listOf(
            Tokens.DARK_ON_ACCENT to Tokens.DARK_ACCENT,
            Tokens.LIGHT_ON_ACCENT to Tokens.LIGHT_ACCENT,
            Tokens.CONTRAST_ON_ACCENT to Tokens.CONTRAST_INK,
        ).forEach { (ink, accent) ->
            val ratio = contrastRatio(ink, accent)
            assertTrue(
                "${ink.toHex()} auf ${accent.toHex()} erreicht nur %.2f:1".format(ratio),
                ratio >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `der Akzent selbst hebt sich vom Hintergrund ab`() {
        assertTrue(contrastRatio(Tokens.DARK_ACCENT, Tokens.DARK_BACKGROUND) >= Tokens.MIN_TILE_ON_BACKGROUND)
        assertTrue(contrastRatio(Tokens.LIGHT_ACCENT, Tokens.LIGHT_BACKGROUND) >= Tokens.MIN_TILE_ON_BACKGROUND)
    }

    private fun Long.toHex() = "#%06X".format(this and 0xFFFFFF)
}
