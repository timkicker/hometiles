package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import androidx.compose.ui.graphics.Color
import java.io.File
import dev.kicker.hometiles.ui.theme.Tokens
import dev.kicker.hometiles.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Druck = Farbe + Haptik" - PLAN.md 3.1, third principle, quoted in the plan's german.
 *
 * there was only a shrink by three percent, and the finger covers exactly that spot at the
 * moment of the press.
 *
 * the pressed surface goes **darker**, never lighter: lighter would mean less distance to the
 * label, and the threshold from 3.3 holds during a press too. the second test does that
 * arithmetic for every tile hue of both themes.
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
    fun `the pressed tile goes visibly darker`() {
        (Tokens.DARK_TILES + Tokens.LIGHT_TILES).forEach { hue ->
            val light = colorOf(hue)
            val dark = darken(light)
            val difference = contrastRatio(hue, asLong(dark))
            assertTrue(
                "on ${hue.toString(16)} the press is hardly visible: $difference to 1",
                difference > 1.45,
            )
        }
    }

    /** the distance to the label must not shrink because of the press. */
    @Test
    fun `the label stays readable during the press`() {
        listOf(
            Tokens.DARK_TILES to 0xFFFFFFFFL,
            Tokens.LIGHT_TILES to 0xFFFFFFFFL,
        ).forEach { (tiles, ink) ->
            tiles.forEach { hue ->
                val before = contrastRatio(ink, hue)
                val after = contrastRatio(ink, asLong(darken(colorOf(hue))))
                assertTrue(
                    "on ${hue.toString(16)} the contrast drops on press from $before to $after",
                    after >= before,
                )
                assertTrue("$after lies under the threshold", after >= Tokens.MIN_LABEL_ON_TILE)
            }
        }
    }

    /** and the border grows - in the high contrast theme the surface is black already. */
    @Test
    fun `in high contrast the border carries the press`() {
        val source = Quelltext.file("dev/kicker/hometiles/ui/BigTile.kt").readText()
        assertTrue("the border does not grow on press", "if (pressed) 2.dp else 0.dp" in source)
    }
}
