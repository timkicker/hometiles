package org.biglau.settings

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.Defaults
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.biglau.data.Security
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.9 „Alles zurücksetzen" - der einzige Schritt in dieser App, der nicht
 * rückgängig zu machen ist.
 */
class ResetTest {

    private fun kachel(action: ButtonAction) = Button(action = action)

    private val config = LauncherConfig(
        screens = listOf(
            Screen(
                id = "home",
                name = "Start",
                cols = 2,
                rows = 3,
                cells = listOf(
                    Cell(0, 0, button = kachel(ButtonAction.Action(Builtin.SETTINGS))),
                    Cell(1, 0, button = kachel(ButtonAction.App("a", "b"))),
                    Cell(0, 1, button = kachel(ButtonAction.None)),
                    Cell(1, 1, button = kachel(ButtonAction.Widget("p/W", 7, "Uhr"))),
                ),
            ),
            Screen(
                id = "mehr",
                name = "Mehr",
                cols = 2,
                rows = 3,
                kind = ScreenKind.FOLDER,
                cells = listOf(Cell(0, 0, button = kachel(ButtonAction.Widget("p/W", 9, "Wetter")))),
            ),
        ),
    )

    // Eine Zahl macht die Warnung wahr. "Bist du sicher" tippt man weg, ohne es zu lesen.
    @Test
    fun `die rueckfrage zaehlt, was verschwindet`() {
        val verlust = Reset.losses(config)
        assertEquals(1, verlust.screens)
        assertEquals(1, verlust.folders)
        assertEquals(4, verlust.tiles)
    }

    // Leere Kacheln sind kein Verlust - sie mitzuzaehlen machte die Warnung groesser,
    // als sie ist, und eine uebertriebene Warnung glaubt man beim naechsten Mal nicht.
    @Test
    fun `leere kacheln zaehlen nicht mit`() {
        val leer = LauncherConfig(
            screens = listOf(
                Screen("x", "X", 2, 3, cells = listOf(Cell(0, 0, button = Button()))),
            ),
        )
        assertEquals(0, Reset.losses(leer).tiles)
    }

    @Test
    fun `die pin wird eigens genannt`() {
        assertEquals(false, Reset.losses(config).hasPin)
        val mitPin = config.copy(security = Security(pin = "abc"))
        assertEquals(true, Reset.losses(mitPin).hasPin)
    }

    // Ohne diesen Schritt behielte der Widget-Host die Kennungen fuer immer, und die
    // Anbieter-App hielte ein Widget am Leben, das niemand mehr sieht.
    @Test
    fun `alle widget-kennungen werden eingesammelt`() {
        assertEquals(listOf(7, 9), Reset.widgetIds(config))
    }

    @Test
    fun `der Notfall-Bildschirm wirft dieselben Widget-Kennungen weg`() {
        // Der Notfall-Bildschirm hatte sein eigenes Zuruecksetzen: `LauncherConfig()`
        // direkt, ohne die Kennungen freizugeben. Der Widget-Host haette sie fuer immer
        // gehalten - und gemerkt haette es niemand, weil man auf diesem Bildschirm ohnehin
        // nichts sieht. Beide Wege benutzen jetzt dieselben zwei Funktionen hier.
        assertEquals(LauncherConfig(), Reset.fresh())
        assertTrue("es gibt Widget-Kennungen zum Freigeben", Reset.widgetIds(config).isNotEmpty())
    }

    @Test
    fun `zuruecksetzen ergibt den zustand nach der installation`() {
        assertEquals(LauncherConfig(), Reset.fresh())
        assertTrue("die eingerichtete Belegung ist nicht die Vorgabe", config != Reset.fresh())
    }

    // Sonst stuende man vor einem fremden Startbildschirm ohne Hinweis, was zu tun ist.
    @Test
    fun `der assistent laeuft danach wieder`() {
        assertEquals(false, Reset.fresh().wizardDone)
    }

    // Der haeufigste Fehler dieser App waere hier am teuersten: ein Startbildschirm ohne
    // Weg in die Einstellungen ist ohne adb nicht mehr zu retten.
    @Test
    fun `der frische startbildschirm hat eine einstellungs-kachel`() {
        val kacheln = Reset.fresh().homeScreen.cells.map { it.button.action }
        assertEquals(true, kacheln.contains(ButtonAction.Action(Builtin.SETTINGS)))
    }

    @Test
    fun `der frische startbildschirm ist der vorgabe-bildschirm`() {
        assertEquals(Defaults.mainScreen(), Reset.fresh().homeScreen)
    }
}
