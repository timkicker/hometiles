package org.biglau.tiles

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
 * PLAN.md 4.1 „Screen … duplizieren".
 *
 * Gedacht zum Ausprobieren: eine neue Anordnung bauen, ohne die alte zu verlieren. Der
 * wichtigste Teil ist deshalb nicht das Kopieren, sondern was nicht mitkopiert wird.
 */
class ScreenCopyTest {

    private fun kachel(action: ButtonAction) = Button(action = action)

    private val voll = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 2,
        cells = listOf(
            Cell(0, 0, button = kachel(ButtonAction.App("a", "b"))),
            Cell(1, 0, button = kachel(ButtonAction.Action(Builtin.SETTINGS))),
            Cell(0, 1, button = kachel(ButtonAction.Widget("p/W", 7, "Uhr"))),
        ),
    )

    private val config = LauncherConfig(screens = listOf(voll), homeScreenId = "home")

    @Test
    fun `die kopie hat dasselbe raster und einen neuen namen`() {
        val ergebnis = ScreenCopy.duplicate(config, "home", "Start (Kopie)")
        val fertig = ergebnis as ScreenCopy.Result.Done
        val kopie = fertig.config.screenById(fertig.newId)!!
        assertEquals("Start (Kopie)", kopie.name)
        assertEquals(2, kopie.cols)
        assertEquals(2, kopie.rows)
    }

    /**
     * Widgets werden nicht mitkopiert. Eine Widget-Kachel hält eine Kennung, die der
     * AppWidgetHost genau einmal vergeben hat; zweimal dieselbe hieße, dass das Löschen
     * der einen Kachel die andere kaputtmacht.
     */
    @Test
    fun `widgets bleiben zurueck und werden gezaehlt`() {
        val fertig = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        val kopie = fertig.config.screenById(fertig.newId)!!
        assertEquals(false, kopie.cells.any { it.button.action is ButtonAction.Widget })
        assertEquals(1, fertig.skippedWidgets)
        assertEquals(2, fertig.copied)
    }

    // Zu einem Ordner gehoert genau eine Kachel; sein Loeschen raeumt beides zusammen weg.
    // Zwei Kacheln auf denselben Ordner liessen nach dem Loeschen eine ins Leere zeigen.
    @Test
    fun `ordnerkacheln bleiben zurueck`() {
        val mitOrdner = config.copy(
            screens = listOf(
                voll.copy(cells = listOf(Cell(0, 0, button = kachel(ButtonAction.Folder("f"))))),
                Screen("f", "Mehr", 2, 2, kind = ScreenKind.FOLDER),
            ),
        )
        val fertig = ScreenCopy.duplicate(mitOrdner, "home", "K") as ScreenCopy.Result.Done
        val kopie = fertig.config.screenById(fertig.newId)!!
        assertEquals(false, kopie.cells.any { it.button.action is ButtonAction.Folder })
        assertEquals(1, fertig.skippedFolders)
    }

    /**
     * Das Wichtigste: die Kopie ist erreichbar. Ein Screen, zu dem keine Kachel führt, ist
     * eingerichtet und unauffindbar — genau der Zustand, vor dem die Screens-Seite warnt.
     */
    @Test
    fun `eine kachel fuehrt zur kopie`() {
        val fertig = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        val ziele = fertig.config.screenById("home")!!.cells
            .mapNotNull { (it.button.action as? ButtonAction.GoToScreen)?.screenId }
        assertTrue(ziele.contains(fertig.newId))
        assertEquals(emptyList<Screen>(), ScreenEdits.unreachable(fertig.config))
    }

    // Lieber gar nicht verdoppeln als eine unauffindbare Kopie hinterlassen.
    @Test
    fun `ohne freie zelle wird nicht verdoppelt`() {
        val randvoll = config.copy(
            screens = listOf(
                voll.copy(
                    cols = 1,
                    rows = 1,
                    cells = listOf(Cell(0, 0, button = kachel(ButtonAction.App("a", "b")))),
                ),
            ),
        )
        assertEquals(
            ScreenCopy.Result.NoRoomForJumpTile,
            ScreenCopy.duplicate(randvoll, "home", "K"),
        )
    }

    @Test
    fun `ein unbekannter screen ergibt nichts`() {
        assertEquals(ScreenCopy.Result.NoSuchScreen, ScreenCopy.duplicate(config, "weg", "K"))
    }

    // Die Kennung muss neu sein, sonst ueberschreibt die Kopie das Original.
    @Test
    fun `die kopie bekommt eine eigene kennung`() {
        val fertig = ScreenCopy.duplicate(config, "home", "K") as ScreenCopy.Result.Done
        assertEquals(2, fertig.config.screens.size)
        assertTrue(fertig.newId != "home")
    }
}
