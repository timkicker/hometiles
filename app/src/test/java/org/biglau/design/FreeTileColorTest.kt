package org.biglau.design

import org.biglau.ui.theme.FreeTileColor
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.relativeLuminance
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine frei gewählte Farbe darf nie unlesbar sein.
 *
 * Genau daran hängt, ob „frei" (PLAN.md 4.2) und „Kontrast ist eine Funktion, kein
 * Geschmack" (3.3) zusammengehen. Der Mensch wählt den Farbton, die Helligkeit rechnet die
 * App — und wenn diese Rechnung für auch nur einen Ton danebengeht, steht eine Beschriftung
 * auf einer Kachel, die man nicht mehr lesen kann.
 */
class FreeTileColorTest {

    private val dunkel = Tokens.DARK_BACKGROUND
    private val hell = Tokens.LIGHT_BACKGROUND
    private val weiss = 0xFFFFFFFFL
    private val gewichtDunkel = FreeTileColor.targetLuminance(Tokens.DARK_TILES)
    private val gewichtHell = FreeTileColor.targetLuminance(Tokens.LIGHT_TILES)

    private fun farbe(ton: Float, hintergrund: Long) = FreeTileColor.forHue(
        ton,
        hintergrund,
        weiss,
        if (hintergrund == dunkel) gewichtDunkel else gewichtHell,
    )

    private fun pruefe(hintergrund: Long, name: String) {
        FreeTileColor.hues.forEach { ton ->
            val farbe = farbe(ton, hintergrund)
            val gegenGrund = contrastRatio(farbe, hintergrund)
            val schrift = contrastRatio(weiss, farbe)
            assertTrue(
                "$name, Ton $ton: Kachel gegen Hintergrund nur $gegenGrund",
                gegenGrund >= Tokens.MIN_TILE_ON_BACKGROUND,
            )
            assertTrue(
                "$name, Ton $ton: Beschriftung auf der Kachel nur $schrift",
                schrift >= Tokens.MIN_LABEL_ON_TILE,
            )
        }
    }

    @Test
    fun `jeder Farbton haelt im dunklen Thema beide Schwellen`() = pruefe(dunkel, "dunkel")

    @Test
    fun `jeder Farbton haelt im hellen Thema beide Schwellen`() = pruefe(hell, "hell")

    @Test
    fun `es gibt vierundzwanzig Toene und keiner doppelt`() {
        assertEquals(24, FreeTileColor.hues.size)
        assertEquals(24, FreeTileColor.hues.toSet().size)
        assertEquals(0f, FreeTileColor.hues.first())
        assertTrue("der letzte bleibt unter 360", FreeTileColor.hues.last() < 360f)
    }

    @Test
    fun `derselbe Ton ergibt immer dieselbe Farbe`() {
        // Sonst spraenge die Kachel bei jedem Neuzeichnen um.
        FreeTileColor.hues.forEach { ton ->
            assertEquals(
                farbe(ton, dunkel),
                farbe(ton, dunkel),
            )
        }
    }

    @Test
    fun `der Ton laeuft rund um den Kreis`() {
        assertEquals(
            farbe(30f, dunkel),
            farbe(390f, dunkel),
        )
        assertEquals(
            farbe(30f, dunkel),
            farbe(-330f, dunkel),
        )
    }

    @Test
    fun `die freien Farben liegen auf dem Gewicht der Palette`() {
        // PLAN.md 3.3: alle Kachelfarben liegen absichtlich auf demselben Niveau. Zuerst
        // maximierte die Rechnung stattdessen den Abstand zu beiden Schwellen - im hellen
        // Thema wandert damit jeder Ton gegen Schwarz, weil dort beide Verhaeltnisse
        // wachsen, je dunkler die Kachel ist. Am Bildschirmfoto gemessen: 14,5:1 statt der
        // angepeilten 3,5, und vierundzwanzig Toene, die alle gleich aussahen.
        listOf(dunkel to gewichtDunkel, hell to gewichtHell).forEach { (grund, ziel) ->
            FreeTileColor.hues.forEach { ton ->
                val abstand = abs(relativeLuminance(farbe(ton, grund)) - ziel)
                assertTrue(
                    "Ton $ton weicht um $abstand vom Gewicht der Palette ab",
                    abstand < 0.06,
                )
            }
        }
    }

    @Test
    fun `auch im hellen Thema sind die Toene auseinanderzuhalten`() {
        // Genau der Fall, den erst das Bildschirmfoto zeigte.
        val farben = FreeTileColor.hues.map { farbe(it, hell) }
        assertTrue("mindestens 20 unterscheidbare Farben", farben.toSet().size >= 20)
        // Und keine davon darf faktisch schwarz sein.
        farben.forEach {
            assertTrue("zu dunkel: ${it.toString(16)}", relativeLuminance(it) > 0.04)
        }
    }

    @Test
    fun `verschiedene Toene ergeben verschiedene Farben`() {
        // Wuerde die Rechnung alles auf denselben Wert ziehen, waere die Wahl eine Attrappe.
        val farben = FreeTileColor.hues.map { farbe(it, dunkel) }
        assertTrue("mindestens 20 unterscheidbare Farben", farben.toSet().size >= 20)
    }

    @Test
    fun `dasselbe Thema gibt jedem Ton eine eigene Fassung, kein Grau`() {
        // Der Rueckfall gibt die Hintergrundfarbe zurueck - der darf nie greifen.
        FreeTileColor.hues.forEach { ton ->
            assertTrue(
                "Ton $ton faellt auf den Hintergrund zurueck",
                farbe(ton, dunkel) != dunkel,
            )
        }
    }
}
