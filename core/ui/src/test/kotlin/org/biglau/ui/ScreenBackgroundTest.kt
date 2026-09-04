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
        for ((thema, systemIsDark) in themenUndSystem()) {
            val palette = paletteFor(thema, systemIsDark)
            for (grund in ScreenBackground.choicesFor(thema, systemIsDark)) {
                for ((i, kachel) in palette.tiles.withIndex()) {
                    val wert = contrastRatio(grund, kachel.value.toLong() shr 32)
                    if (wert < 3.0) schwach += "$thema/Kachel$i/${grund.toString(16)}: %.2f".format(wert)
                }
            }
        }
        assertEquals(emptyList<String>(), schwach)
    }

    /**
     * Auf einem eigenen Hintergrund steht **mehr** als eine Kachel.
     *
     * Geprüft war bis zum 04.09.2026 nur die Kachel. Daneben liegen dort aber auch der
     * Rahmen der leeren Kachel — der einzige Hinweis, dass da ein freier Platz ist — und
     * die Warnschrift. Beide sind gegen die *Themafarbe* geprüft, und genau diese Prüfung
     * macht ein eigener Hintergrund ungültig; das steht wörtlich im Kommentar von
     * `ScreenBackground.inkFor`, war aber nur für die Tinte gedacht.
     *
     * Nachgerechnet halten beide — knapp: der Rahmen kommt im dunklen Thema auf 3,06, die
     * Warnschrift auf 7,23. Bei zwölf Prozent Einfärbung ist das kein Zufall, sondern die
     * Zahl, die dort gewählt wurde. Wer sie erhöht oder eine der beiden Farben anfasst,
     * soll es hier merken und nicht am Gerät.
     */
    @Test
    fun `auch Rahmen und Warnschrift halten auf jedem eigenen Hintergrund`() {
        val schwach = mutableListOf<String>()
        for ((thema, systemIsDark) in themenUndSystem()) {
            val palette = paletteFor(thema, systemIsDark)
            for (grund in ScreenBackground.choicesFor(thema, systemIsDark)) {
                val rahmen = contrastRatio(palette.emptyTileBorder.value.toLong() shr 32, grund)
                if (rahmen < 3.0) {
                    schwach += "$thema/Rahmen/${grund.toString(16)}: %.2f".format(rahmen)
                }
                val warnung = contrastRatio(palette.dangerText.value.toLong() shr 32, grund)
                if (warnung < 7.0) {
                    schwach += "$thema/Warnschrift/${grund.toString(16)}: %.2f".format(warnung)
                }
            }
        }
        assertEquals(emptyList<String>(), schwach)
    }

    // Die Tinte auf dem Hintergrund wird neu entschieden, nicht vom Thema uebernommen -
    // die Tinte des Themas ist gegen die Themafarbe geprueft, nicht gegen diese hier.
    @Test
    fun `die tinte erreicht ueberall den grosstext-wert`() {
        for (grund in themenUndSystem().flatMap { (thema, dunkel) -> ScreenBackground.choicesFor(thema, dunkel) }) {
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
     * Im Hochkontrast-Thema gibt es keine Wahl. Dort tragen Hintergrund und Kacheln
     * dieselbe Farbe — die Kacheln stehen durch ihren Rand da, nicht durch ihre Füllung.
     * Eine eigene Hintergrundfarbe würde genau die eine Eigenschaft aufweichen, wegen der
     * jemand dieses Thema wählt.
     */
    @Test
    fun `der hochkontrast-modus bekommt keine farben angeboten`() {
        assertEquals(false, ScreenBackground.offersChoices(ThemeName.HIGH_CONTRAST))
        assertEquals(emptyList<Long>(), ScreenBackground.choicesFor(ThemeName.HIGH_CONTRAST, true))
        assertTrue(ScreenBackground.choicesFor(ThemeName.DARK, true).isNotEmpty())
        assertTrue(ScreenBackground.choicesFor(ThemeName.LIGHT, true).isNotEmpty())
        // Auch "wie das Telefon" bekommt Farben - in beiden Zustaenden.
        assertTrue(ScreenBackground.choicesFor(ThemeName.SYSTEM, true).isNotEmpty())
        assertTrue(ScreenBackground.choicesFor(ThemeName.SYSTEM, false).isNotEmpty())
    }

    // Die Farben muessen auch voneinander unterscheidbar sein - fuenf Toene, die man nicht
    // auseinanderhaelt, sind keine Auswahl, sondern eine Zumutung.
    @Test
    fun `die farben unterscheiden sich voneinander`() {
        val farben = ScreenBackground.choicesFor(ThemeName.DARK, true)
        assertEquals(farben.size, farben.toSet().size)
    }
}
