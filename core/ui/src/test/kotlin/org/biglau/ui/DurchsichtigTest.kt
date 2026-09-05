package org.biglau.ui

import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine halbdurchsichtige Farbe ist eine neue Farbe — und die hat noch niemand geprüft.
 *
 * `ContrastTest` und `SurfaceContrastTest` rechnen mit den Tokens. Ein `.copy(alpha = …)`
 * kommt in keiner Palette vor: es entsteht erst beim Zeichnen, aus zwei Farben. Am
 * 04.09.2026 sind alle sieben Stellen im Quelltext durchgegangen worden, und zwei fielen
 * durch:
 *
 * * Der **Rand des leeren PIN-Punktes** (0,35): hell 2,23, Kontrast 2,64 — unter 3,0. Die
 *   Füllung des Punktes ist absichtlich still (1,09 bis 1,16 gegen den Hintergrund), der
 *   Rand trägt ihn also allein. Am Gerät bestätigt: (157,158,160) auf (232,234,236).
 * * Der Hinweis **„Zum Schliessen tippen"** (0,7): hell 6,92 — knapp unter 7,0, der Schwelle
 *   für Schrift auf dem Hintergrund.
 *
 * Zwei weitere sind gemessen und **bleiben**: die Spur unter dem Akkubalken und die unter den
 * Empfangsstufen stehen mit 1,7 bis 2,1 gegen ihren Grund, aber die Aussage steckt im
 * Unterschied zwischen gefüllt und leer (3,4 bis 9,3), und beim Akku steht die Zahl daneben.
 * Eine Spur ist kein Zeichen, sondern der Platz, den ein Zeichen einnehmen kann.
 */
class DurchsichtigTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    private fun ueber(vorne: Long, hinten: Long, anteil: Float): Long {
        var ergebnis = 0xFFL shl 24
        listOf(16, 8, 0).forEach { schiebung ->
            val v = (vorne shr schiebung) and 0xFF
            val h = (hinten shr schiebung) and 0xFF
            ergebnis = ergebnis or (Math.round(anteil * v + (1 - anteil) * h).toLong() shl schiebung)
        }
        return ergebnis
    }

    private val themen = listOf(ThemeName.DARK, ThemeName.LIGHT, ThemeName.HIGH_CONTRAST)

    @Test
    fun `der rand des leeren punktes ist zu finden`() {
        themen.forEach { thema ->
            val palette = paletteFor(thema, false)
            val fuellung = palette.emptyTile.argb()
            val rand = ueber(palette.onBackground.argb(), fuellung, DOT_BORDER)
            val ratio = contrastRatio(rand, fuellung)
            assertTrue(
                ("%s: der Rand des leeren PIN-Punktes erreicht nur %.2f:1 - und seine " +
                    "Fuellung steht gegen den Hintergrund still (%.2f:1), er traegt den " +
                    "Punkt also allein").format(
                    thema,
                    ratio,
                    contrastRatio(fuellung, palette.background.argb()),
                ),
                ratio >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
        }
    }
}
