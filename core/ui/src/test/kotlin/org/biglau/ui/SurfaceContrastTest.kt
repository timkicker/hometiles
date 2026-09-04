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
        themenUndSystem().forEach { (theme, systemIsDark) ->
            paletteFor(theme, systemIsDark).allSurfaces().forEach { surface ->
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
        themenUndSystem().filter { it.first != ThemeName.HIGH_CONTRAST }.forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
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

    /**
     * Ein Rahmen hat **zwei** Gruende: aussen den Hintergrund, innen die Fuellung der
     * Kachel, um die er liegt. Bis zum 04.09.2026 stand hier nur der aeussere - und innen
     * kam der helle Rahmen auf 2,86:1, unter der Schwelle. Am Emulator nachgesehen, Bildpunkt
     * fuer Bildpunkt: bei x=243..245 der Rahmen, ab x=246 die Fuellung, ohne Zwischenraum.
     *
     * Der Fehler ist derselbe wie beim Warnrot zwei Stunden vorher: die Regel gab es, sie
     * war nur an einem von zwei Gruenden gemessen.
     */
    @Test
    fun `die leere Kachel ist ueber ihren Rahmen auffindbar`() {
        // Die Fuellung ist absichtlich still (1,09:1 im dunklen Thema). Damit ein leerer
        // Platz trotzdem sichtbar ist, muss der Rahmen die Flaechenschwelle erreichen.
        themenUndSystem().forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            listOf(
                "aussen, gegen den Hintergrund" to palette.background,
                "innen, gegen die Fuellung" to palette.emptyTile,
            ).forEach { (wo, grund) ->
                val ratio = contrastRatio(palette.emptyTileBorder.argb(), grund.argb())
                assertTrue(
                    "%s: Rahmen %s erreicht %s nur %.2f:1".format(
                        theme, palette.emptyTileBorder.hex(), wo, ratio,
                    ),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    /**
     * Die Warnschrift steht nicht in [org.biglau.ui.theme.BigPalette.allSurfaces] - sie ist
     * keine Flaeche, sondern ein Ton fuer sich. `ContrastTest` prueft die Tokens; hier wird
     * die **Palette** geprueft, also auch, dass jedes Thema den richtigen Token verdrahtet
     * hat. Ein Thema, das versehentlich `danger` einsetzt, faellt sonst nirgends auf.
     */
    @Test
    fun `die Warnschrift jedes Themas liegt ueber der strengen Schwelle`() {
        themenUndSystem().forEach { (theme, systemIsDark) ->
            val palette = paletteFor(theme, systemIsDark)
            listOf(
                "auf dem Hintergrund" to palette.background,
                "auf der leeren Kachel" to palette.emptyTile,
            ).forEach { (wo, grund) ->
                val ratio = contrastRatio(palette.dangerText.argb(), grund.argb())
                assertTrue(
                    "%s: Warnschrift %s erreicht %s nur %.2f:1".format(
                        theme, palette.dangerText.hex(), wo, ratio,
                    ),
                    ratio >= Tokens.MIN_TEXT_ON_BACKGROUND,
                )
            }
        }
    }

    @Test
    fun `jedes Thema bietet alle Flaechen an`() {
        themenUndSystem().forEach { (theme, systemIsDark) ->
            // drei benannte Flaechen plus sechs Kachelfarben
            assertTrue(paletteFor(theme, systemIsDark).allSurfaces().size == 9)
        }
    }

    private fun Color.hex() = "#%06X".format(argb() and 0xFFFFFF)
}
