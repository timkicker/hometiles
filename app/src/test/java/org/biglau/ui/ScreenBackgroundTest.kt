package org.biglau.ui

import org.biglau.data.Background
import org.biglau.data.Screen
import org.biglau.ui.theme.ScreenBackground
import org.biglau.data.ThemeName
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1: eigener Hintergrund pro Screen.
 *
 * `Background.Solid` wurde von Anfang an gemalt und war nirgends einzustellen;
 * `Background.Image` stand im Modell und wurde nie gemalt — ein `else`-Zweig verschluckte
 * ihn. Beides sind Zusagen, die in der Sicherungsdatei stehen und nichts tun.
 */
class ScreenBackgroundTest {

    @Test
    fun `die vorgabe ist die farbe des themas`() {
        assertEquals(Background.Theme, Screen("a", "A", 2, 3).background)
    }

    /**
     * Die eine Regel, an der alles hängt: eine Kachel muss sich vom Untergrund abheben,
     * sonst sieht man nicht, wo sie aufhört. Drei zu eins ist die WCAG-Schwelle für
     * Flächen, die man auseinanderhalten muss.
     */
    @Test
    fun `jede farbe hebt sich von jeder kachelfarbe ab`() {
        val schwach = mutableListOf<String>()
        for (thema in ThemeName.entries) {
            val palette = paletteFor(thema)
            for (grund in ScreenBackground.choicesFor(thema)) {
                for ((i, kachel) in palette.tiles.withIndex()) {
                    val wert = contrastRatio(grund, kachel.value.toLong() shr 32)
                    if (wert < 3.0) schwach += "$thema/Kachel$i/${grund.toString(16)}: %.2f".format(wert)
                }
            }
        }
        assertEquals(emptyList<String>(), schwach)
    }

    // Die Tinte auf dem Hintergrund wird neu entschieden, nicht vom Thema uebernommen -
    // die Tinte des Themas ist gegen die Themafarbe geprueft, nicht gegen diese hier.
    @Test
    fun `die tinte erreicht ueberall den grosstext-wert`() {
        for (grund in ThemeName.entries.flatMap { ScreenBackground.choicesFor(it) }) {
            val tinte = ScreenBackground.inkFor(grund)
            assertTrue(
                "${grund.toString(16)} erreicht nur %.2f".format(contrastRatio(grund, tinte)),
                contrastRatio(grund, tinte) >= 4.5,
            )
        }
    }

    @Test
    fun `auf dunklem grund steht helle tinte`() {
        assertEquals(0xFFFFFFFF, ScreenBackground.inkFor(0xFF101418))
        assertEquals(0xFF000000, ScreenBackground.inkFor(0xFFFFEB3B))
    }

    /**
     * Kein Bild. Hinter einem Foto ist kein Kontrast planbar — an jeder Stelle des Bildes
     * anders —, und genau die Prüfungen oben wären damit hinfällig. Der Test hält fest,
     * dass der Fall gar nicht erst im Modell steht: ein Fall, den niemand behandelt, ist
     * schlimmer als einer, den es nicht gibt.
     */
    @Test
    fun `es gibt genau zwei arten von hintergrund`() {
        val modell = java.io.File("src/main/java/org/biglau/data/Model.kt").readText()
        val abschnitt = modell.substringAfter("sealed interface Background {")
            .substringBefore("}")
        assertEquals(true, abschnitt.contains("Theme"))
        assertEquals(true, abschnitt.contains("Solid"))
        assertEquals(false, abschnitt.contains("Image"))
    }

    /**
     * Im Hochkontrast-Thema gibt es keine Wahl. Dort tragen Hintergrund und Kacheln
     * dieselbe Farbe — die Kacheln stehen durch ihren Rand da, nicht durch ihre Füllung.
     * Eine eigene Hintergrundfarbe würde genau die eine Eigenschaft aufweichen, wegen der
     * jemand dieses Thema wählt.
     */
    @Test
    fun `der hochkontrast-modus bekommt keine farben angeboten`() {
        assertEquals(false, ScreenBackground.offersChoices(ThemeName.HIGH_CONTRAST))
        assertEquals(emptyList<Long>(), ScreenBackground.choicesFor(ThemeName.HIGH_CONTRAST))
        assertTrue(ScreenBackground.choicesFor(ThemeName.DARK).isNotEmpty())
        assertTrue(ScreenBackground.choicesFor(ThemeName.LIGHT).isNotEmpty())
    }

    // Die Farben muessen auch voneinander unterscheidbar sein - fuenf Toene, die man nicht
    // auseinanderhaelt, sind keine Auswahl, sondern eine Zumutung.
    @Test
    fun `die farben unterscheiden sich voneinander`() {
        val farben = ScreenBackground.choicesFor(ThemeName.DARK)
        assertEquals(farben.size, farben.toSet().size)
    }
}
