package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blass heisst nicht unsichtbar.
 *
 * Am Listenende wird der Blätterknopf blass — „ein Knopf, der aussieht wie immer und nichts
 * tut, lässt einen an der eigenen Bedienung zweifeln", sagt sein Kommentar. Wer aber nicht
 * *sieht*, dass er noch da ist, sucht ihn.
 *
 * Ein Symbol ist eine Fläche, keine Schrift, also gilt `MIN_TILE_ON_BACKGROUND` (3,0). Die
 * Zahl dahinter hat bis zum 04.09.2026 niemand nachgerechnet: `alpha = 0.4f` stand einfach
 * da. Nachgerechnet ergibt das dunkel 3,81 und im Kontrastthema 3,18 — **im hellen Thema
 * aber 2,59**, ein `#999999` auf `#F3F4F4`. Am Emulator im Bildpunkt bestätigt: 153,153,153.
 *
 * Diese Regel rechnet das Übereinanderlegen selbst nach, statt einer Zahl zu glauben.
 */
class BlasserKnopfTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    /** Was aus [vorne] wird, wenn es mit [anteil] Deckung auf [hinten] liegt. */
    private fun ueber(vorne: Long, hinten: Long, anteil: Float): Long {
        var ergebnis = 0xFFL shl 24
        listOf(16, 8, 0).forEach { schiebung ->
            val v = (vorne shr schiebung) and 0xFF
            val h = (hinten shr schiebung) and 0xFF
            val wert = Math.round(anteil * v + (1 - anteil) * h).toLong()
            ergebnis = ergebnis or (wert shl schiebung)
        }
        return ergebnis
    }

    @Test
    fun `der blasse knopf bleibt sichtbar`() {
        listOf(
            ThemeName.DARK to false,
            ThemeName.LIGHT to false,
            ThemeName.HIGH_CONTRAST to false,
        ).forEach { (thema, systemIstDunkel) ->
            val palette = paletteFor(thema, systemIstDunkel)
            val grund = palette.background.argb()
            val flaeche = palette.surfaceDefault.fill.argb()
            val schrift = palette.surfaceDefault.ink.argb()

            val blasseFlaeche = ueber(flaeche, grund, BLASS)
            val blassesSymbol = ueber(schrift, blasseFlaeche, BLASS)

            listOf(
                "gegen die blasse Flaeche" to blasseFlaeche,
                "gegen den Hintergrund" to grund,
            ).forEach { (wo, dahinter) ->
                val ratio = contrastRatio(blassesSymbol, dahinter)
                assertTrue(
                    "%s: das blasse Symbol (#%06X) erreicht %s nur %.2f:1 - unter %.1f"
                        .format(thema, blassesSymbol and 0xFFFFFF, wo, ratio, Tokens.MIN_TILE_ON_BACKGROUND),
                    ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
                )
            }
        }
    }

    @Test
    fun `blass ist trotzdem deutlich blasser als wach`() {
        // Sonst waere die Ruecknahme keine: wer nicht sieht, dass der Knopf nichts mehr tut,
        // tippt ihn weiter an.
        listOf(ThemeName.DARK, ThemeName.LIGHT, ThemeName.HIGH_CONTRAST).forEach { thema ->
            val palette = paletteFor(thema, false)
            val flaeche = palette.surfaceDefault.fill.argb()
            val schrift = palette.surfaceDefault.ink.argb()
            val blasseFlaeche = ueber(flaeche, palette.background.argb(), BLASS)
            val blassesSymbol = ueber(schrift, blasseFlaeche, BLASS)
            val wach = contrastRatio(schrift, flaeche)
            val blass = contrastRatio(blassesSymbol, blasseFlaeche)
            assertTrue(
                "%s: wach %.2f, blass %.2f - der Unterschied traegt nicht".format(thema, wach, blass),
                wach >= blass * 2,
            )
        }
    }
}
