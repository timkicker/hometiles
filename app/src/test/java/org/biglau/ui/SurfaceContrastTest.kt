package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.BigSurface
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Laeuft ueber jedes Flaechenpaar jedes Themas. Dreimal ist hier dieselbe Sorte Fehler
 * entstanden - weisser Text auf einer hellen Flaeche - immer an einer neuen Stelle.
 * Dieser Test kennt keine Stellen, nur Paare, und faengt deshalb auch die naechste.
 */
class SurfaceContrastTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    private fun BigSurface.ratio(): Double = contrastRatio(ink.argb(), fill.argb())

    @Test
    fun `jede Flaeche traegt ihre eigene Schrift lesbar`() {
        ThemeName.entries.forEach { theme ->
            paletteFor(theme).allSurfaces().forEach { surface ->
                val ratio = surface.ratio()
                assertTrue(
                    "%s: %s auf %s erreicht nur %.2f:1".format(
                        theme, surface.ink.hex(), surface.fill.hex(), ratio,
                    ),
                    ratio >= Tokens.MIN_LABEL_ON_TILE,
                )
            }
        }
    }

    @Test
    fun `jede bedeutungstragende Flaeche hebt sich vom Hintergrund ab`() {
        // Gilt fuer Fuellungen, die selbst die Aussage tragen: Kacheln, Akzent, Warnung.
        // Die stille Flaeche unter einer Listenzeile ist davon ausgenommen - dort
        // identifiziert der Text die Zeile, nicht die Fuellung.
        ThemeName.entries.filter { it != ThemeName.HIGH_CONTRAST }.forEach { theme ->
            val palette = paletteFor(theme)
            val background = palette.background.argb()
            val meaningful = palette.tiles.indices.map(palette::surfaceTile) +
                palette.surfaceAccent + palette.surfaceDanger
            meaningful.forEach { surface ->
                val ratio = contrastRatio(surface.fill.argb(), background)
                assertTrue(
                    "%s: Flaeche %s erreicht nur %.2f:1 gegen den Hintergrund".format(
                        theme, surface.fill.hex(), ratio,
                    ),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    @Test
    fun `die leere Kachel ist ueber ihren Rahmen auffindbar`() {
        // Die Fuellung ist absichtlich still (1,09:1 im dunklen Thema). Damit ein leerer
        // Platz trotzdem sichtbar ist, muss der Rahmen die Flaechenschwelle erreichen.
        ThemeName.entries.forEach { theme ->
            val palette = paletteFor(theme)
            val ratio = contrastRatio(palette.emptyTileBorder.argb(), palette.background.argb())
            assertTrue(
                "%s: Rahmen %s erreicht nur %.2f:1".format(theme, palette.emptyTileBorder.hex(), ratio),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
    }

    @Test
    fun `jedes Thema bietet alle Flaechen an`() {
        ThemeName.entries.forEach { theme ->
            // drei benannte Flaechen plus sechs Kachelfarben
            assertTrue(paletteFor(theme).allSurfaces().size == 9)
        }
    }

    private fun Color.hex() = "#%06X".format(argb() and 0xFFFFFF)
}
