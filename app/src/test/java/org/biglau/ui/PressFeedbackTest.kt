package org.biglau.ui

import androidx.compose.ui.graphics.Color
import java.io.File
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Druck = Farbe + Haptik" — `PLAN.md` 3.1, Leitsatz 3.
 *
 * Da war nur ein Schrumpfen um drei Prozent, und genau die Stelle verdeckt im Moment des
 * Drückens der Finger. Farbe stand im Leitsatz und fehlte im Code; gefunden beim Abgleich
 * Plan gegen Quelltext.
 *
 * Die gedrückte Fläche wird **dunkler**, nie heller: heller hieße weniger Abstand zur
 * Beschriftung, und die Schwelle aus 3.3 gilt auch während eines Drucks. Genau das rechnet
 * der zweite Test nach — für jeden Kachelton beider Themen.
 */
class PressFeedbackTest {

    private fun asLong(c: Color): Long {
        fun k(v: Float) = (v * 255f + 0.5f).toInt().toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (k(c.red) shl 16) or (k(c.green) shl 8) or k(c.blue)
    }

    private fun colorOf(argb: Long) = Color(
        red = ((argb shr 16) and 0xFF).toFloat() / 255f,
        green = ((argb shr 8) and 0xFF).toFloat() / 255f,
        blue = (argb and 0xFF).toFloat() / 255f,
    )

    @Test
    fun `die gedrueckte Kachel wird sichtbar dunkler`() {
        (Tokens.DARK_TILES + Tokens.LIGHT_TILES).forEach { ton ->
            val hell = colorOf(ton)
            val dunkel = darken(hell)
            val unterschied = contrastRatio(ton, asLong(dunkel))
            assertTrue(
                "Auf ${ton.toString(16)} ist der Druck kaum zu sehen: $unterschied zu 1",
                unterschied > 1.45,
            )
        }
    }

    /** Der Abstand zur Beschriftung darf durch den Druck nicht kleiner werden. */
    @Test
    fun `die Beschriftung bleibt lesbar waehrend des Drucks`() {
        listOf(
            Tokens.DARK_TILES to 0xFFFFFFFFL,
            Tokens.LIGHT_TILES to 0xFFFFFFFFL,
        ).forEach { (tiles, ink) ->
            tiles.forEach { ton ->
                val vorher = contrastRatio(ink, ton)
                val nachher = contrastRatio(ink, asLong(darken(colorOf(ton))))
                assertTrue(
                    "Auf ${ton.toString(16)} sinkt der Kontrast beim Druck von $vorher auf $nachher",
                    nachher >= vorher,
                )
                assertTrue("$nachher liegt unter der Schwelle", nachher >= Tokens.MIN_LABEL_ON_TILE)
            }
        }
    }

    /** Und der Rand wächst - im Hochkontrast-Thema ist die Fläche schon schwarz. */
    @Test
    fun `im Hochkontrast traegt der Rand den Druck`() {
        val quelle = File("src/main/java/org/biglau/ui/BigTile.kt").readText()
        assertTrue("Der Rand waechst beim Druck nicht", "if (pressed) 2.dp else 0.dp" in quelle)
    }
}
