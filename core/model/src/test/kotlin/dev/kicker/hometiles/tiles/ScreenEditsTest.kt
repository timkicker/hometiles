package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.Defaults
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenEditsTest {

    private val home = Defaults.mainScreen()
    private val base = LauncherConfig(screens = listOf(home), homeScreenId = Defaults.MAIN_ID)

    @Test
    fun `a new screen always has a way back`() {
        // a launcher must not swallow the back gesture. without a home tile the new screen
        // would be a dead end.
        val screen = ScreenEdits.newScreen("s2", "Second", home)
        val actions = screen.cells.map { it.button.action }
        assertTrue(actions.contains(ButtonAction.Action(Builtin.HOME_SCREEN)))
    }

    @Test
    fun `the home tile sits on the last spot`() {
        val screen = ScreenEdits.newScreen("s2", "Second", home)
        val cell = screen.cells.first()
        assertEquals(home.cols - 1, cell.x)
        assertEquals(home.rows - 1, cell.y)
    }

    @Test
    fun `a new screen takes the grid of the old one`() {
        val wide = Screen(id = "a", name = "A", cols = 3, rows = 4)
        val screen = ScreenEdits.newScreen("s2", "Second", wide)
        assertEquals(3, screen.cols)
        assertEquals(4, screen.rows)
        assertEquals(2, screen.cells.first().x)
        assertEquals(3, screen.cells.first().y)
    }

    @Test
    fun `a new id never lands on an existing screen`() {
        val config = base.copy(screens = base.screens + Screen(id = "screen2", name = "X"))
        val id = ScreenEdits.freeId(config)
        assertTrue(config.screens.none { it.id == id })
    }

    @Test
    fun `the same id is not added twice`() {
        val once = ScreenEdits.add(base, Screen(id = "s2", name = "Second"))
        val twice = ScreenEdits.add(once, Screen(id = "s2", name = "Another name"))
        assertEquals(2, twice.screens.size)
        assertEquals("Second", twice.screens.last().name)
    }

    @Test
    fun `renaming trims and refuses the empty`() {
        val renamed = ScreenEdits.rename(base, Defaults.MAIN_ID, "  At home  ")
        assertEquals("At home", renamed.homeScreen.name)
        assertEquals(renamed, ScreenEdits.rename(renamed, Defaults.MAIN_ID, "   "))
    }

    @Test
    fun `the home screen cannot be deleted`() {
        assertEquals(base, ScreenEdits.delete(base, Defaults.MAIN_ID))
    }

    @Test
    fun `the last screen cannot be deleted`() {
        val single = LauncherConfig(screens = listOf(Screen(id = "only", name = "Only")), homeScreenId = "other")
        assertEquals(single, ScreenEdits.delete(single, "only"))
    }

    @Test
    fun `deleting removes the tiles that jumped there`() {
        // otherwise a tile would stay that does nothing when tapped.
        val withLink = base.copy(
            screens = listOf(
                home.copy(cells = home.cells + Cell(0, 0, button = Button(ButtonAction.GoToScreen("s2")))),
                Screen(id = "s2", name = "Second"),
            ),
        )
        val after = ScreenEdits.delete(withLink, "s2")
        assertEquals(1, after.screens.size)
        assertTrue(after.screens.first().cells.none { it.button.action == ButtonAction.GoToScreen("s2") })
    }

    @Test
    fun `deleting leaves tiles to other screens alone`() {
        val config = base.copy(
            screens = listOf(
                home.copy(cells = listOf(Cell(0, 0, button = Button(ButtonAction.GoToScreen("s3"))))),
                Screen(id = "s2", name = "Second"),
                Screen(id = "s3", name = "Third"),
            ),
        )
        val after = ScreenEdits.delete(config, "s2")
        assertTrue(after.screens.first().cells.any { it.button.action == ButtonAction.GoToScreen("s3") })
    }

    @Test
    fun `deleting tidies the swipe order too`() {
        val config = base.copy(
            screens = listOf(home, Screen(id = "s2", name = "Zweiter")),
            swipeOrder = listOf(Defaults.MAIN_ID, "s2"),
        )
        assertEquals(listOf(Defaults.MAIN_ID), ScreenEdits.delete(config, "s2").swipeOrder)
    }

    @Test
    fun `after deleting no tile points into the void`() {
        val config = base.copy(
            screens = listOf(
                home.copy(cells = listOf(Cell(0, 0, button = Button(ButtonAction.GoToScreen("s2"))))),
                Screen(id = "s2", name = "Second"),
            ),
        )
        val after = ScreenEdits.delete(config, "s2")
        assertTrue(
            after.screens.flatMap { it.cells }
                .none { (it.button.action as? ButtonAction.GoToScreen)?.screenId == "s2" },
        )
    }

}
