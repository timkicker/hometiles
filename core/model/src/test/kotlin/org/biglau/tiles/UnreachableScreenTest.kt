package org.biglau.tiles

import org.biglau.data.Behaviour
import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bildschirme, zu denen kein Weg führt — und Warnungen, die nicht stimmen.
 *
 * Der Fall stammt vom Gerät des Nutzers, 3.9.2026: `Screen 2` war eingerichtet, das Wischen
 * zwischen den Screens aus, keine Kachel sprang hinüber. Sieben freie Felder, unerreichbar.
 * Die App warnt davor — er hat die Warnung nur nie gesehen, weil er gar nicht in die
 * Einstellungen kam (siehe `STATUS.md`, 05:25).
 *
 * Die Gegenrichtung ist genauso wichtig und war falsch: eine Kachel „nächster Bildschirm"
 * blättert durch dieselbe Reihe wie das Wischen, **auch wenn das Wischen aus ist**. Wer sie
 * hat, kommt überall hin und bekam trotzdem die Warnung. Der Kommentar in `unreachable`
 * sagt selbst, warum das schlimm ist: eine Warnung, die nicht stimmt, nimmt man auch dort
 * nicht mehr ernst, wo sie stimmt.
 */
class UnreachableScreenTest {

    private fun zelle(x: Int, y: Int, action: ButtonAction) =
        Cell(x = x, y = y, button = Button(action = action))

    private fun config(kachelAufStart: ButtonAction? = null, wischen: Boolean = false) =
        LauncherConfig(
            screens = listOf(
                Screen(
                    id = "home",
                    name = "Start",
                    cells = listOfNotNull(kachelAufStart?.let { zelle(0, 0, it) }),
                ),
                Screen(id = "zwei", name = "Screen 2"),
            ),
            homeScreenId = "home",
            behaviour = Behaviour(swipeBetweenScreens = wischen),
        )

    @Test
    fun `ohne wischen und ohne sprungkachel ist der zweite bildschirm unerreichbar`() {
        assertEquals(listOf("zwei"), ScreenEdits.unreachable(config()).map { it.id })
    }

    @Test
    fun `mit wischen ist er erreichbar`() {
        assertTrue(ScreenEdits.unreachable(config(wischen = true)).isEmpty())
    }

    @Test
    fun `eine sprungkachel genuegt`() {
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.GoToScreen("zwei"))).isEmpty(),
        )
    }

    @Test
    fun `eine blaetterkachel genuegt ebenfalls`() {
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.Action(Builtin.NEXT_SCREEN))).isEmpty(),
        )
        assertTrue(
            ScreenEdits.unreachable(config(ButtonAction.Action(Builtin.PREV_SCREEN))).isEmpty(),
        )
    }

    @Test
    fun `eine kachel zurueck zum start macht den bildschirm nicht erreichbar`() {
        val wieBeimNutzer = LauncherConfig(
            screens = listOf(
                Screen(id = "home", name = "Start"),
                Screen(
                    id = "zwei",
                    name = "Screen 2",
                    cells = listOf(zelle(1, 2, ButtonAction.Action(Builtin.HOME_SCREEN))),
                ),
            ),
            homeScreenId = "home",
            behaviour = Behaviour(swipeBetweenScreens = false),
        )
        assertEquals(listOf("zwei"), ScreenEdits.unreachable(wieBeimNutzer).map { it.id })
    }

    @Test
    fun `der startbildschirm und ordner zaehlen nie mit`() {
        val config = LauncherConfig(
            screens = listOf(
                Screen(id = "home", name = "Start"),
                Screen(id = "ordner", name = "Mehr", kind = ScreenKind.FOLDER),
            ),
            homeScreenId = "home",
        )
        assertTrue(ScreenEdits.unreachable(config).isEmpty())
    }
}
