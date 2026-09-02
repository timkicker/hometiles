package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kacheln zwischen Screens und Ordnern verschieben.
 *
 * Der Fall, den es zu verhindern gilt: eine Kachel, die beim Verschieben still verschwindet,
 * weil am Ziel kein Platz war. Lieber gar nicht verschieben und es sagen.
 */
class TileMoveTest {

    private fun kachel(x: Int, y: Int, b: Builtin) =
        Cell(x, y, button = Button(action = ButtonAction.Action(b)))

    private val heim = Screen(
        id = "home",
        name = "Start",
        cols = 2,
        rows = 3,
        cells = listOf(kachel(0, 0, Builtin.DIALER), kachel(1, 0, Builtin.CAMERA)),
    )

    private val ordner = Screen(
        id = "f1",
        name = "Bank",
        cols = 2,
        rows = 3,
        kind = ScreenKind.FOLDER,
        cells = listOf(kachel(0, 0, Builtin.CLOCK)),
    )

    private val config = LauncherConfig(screens = listOf(heim, ordner), homeScreenId = "home")

    @Test
    fun `eine Kachel wandert in den Ordner`() {
        val danach = TileMove.move(config, "home", 1, 0, "f1")!!
        assertEquals(1, danach.screens.first { it.id == "home" }.cells.size)
        val inhalt = danach.screens.first { it.id == "f1" }.cells
        assertEquals(2, inhalt.size)
        assertTrue(inhalt.any { (it.button.action as? ButtonAction.Action)?.builtin == Builtin.CAMERA })
    }

    @Test
    fun `sie landet auf dem ersten freien Platz`() {
        // Im Ordner ist (0,0) belegt, also kommt sie auf (1,0).
        val danach = TileMove.move(config, "home", 1, 0, "f1")!!
        val neu = danach.screens.first { it.id == "f1" }.cells
            .first { (it.button.action as? ButtonAction.Action)?.builtin == Builtin.CAMERA }
        assertEquals(1, neu.x)
        assertEquals(0, neu.y)
    }

    @Test
    fun `und auch wieder heraus`() {
        val danach = TileMove.move(config, "f1", 0, 0, "home")!!
        assertTrue(danach.screens.first { it.id == "f1" }.cells.isEmpty())
        assertEquals(3, danach.screens.first { it.id == "home" }.cells.size)
    }

    @Test
    fun `eine breite Kachel kommt einfeldrig an`() {
        // Sonst raegte sie am Ziel ueber den Rand oder ueberdeckte eine belegte Zelle.
        val breit = heim.copy(cells = listOf(Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER)))))
        val c = config.copy(screens = listOf(breit, ordner))
        val neu = TileMove.move(c, "home", 0, 0, "f1")!!
            .screens.first { it.id == "f1" }.cells.first { it.x == 1 && it.y == 0 }
        assertEquals(1, neu.w)
        assertEquals(1, neu.h)
    }

    @Test
    fun `ohne Platz wird nicht verschoben`() {
        val voll = ordner.copy(
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> kachel(x, y, Builtin.CLOCK) } },
        )
        assertNull(TileMove.move(config.copy(screens = listOf(heim, voll)), "home", 1, 0, "f1"))
    }

    @Test
    fun `ein Ordner wandert nicht in einen Ordner`() {
        val mitOrdner = heim.copy(
            cells = heim.cells + Cell(0, 1, button = Button(action = ButtonAction.Folder("f1"))),
        )
        val c = config.copy(screens = listOf(mitOrdner, ordner))
        assertNull(TileMove.move(c, "home", 0, 1, "f1"))
        assertTrue(TileMove.targetsFor(c, "home", mitOrdner.cells.last()).none { it.isFolder })
    }

    @Test
    fun `auf denselben Screen verschiebt nichts`() {
        assertNull(TileMove.move(config, "home", 1, 0, "home"))
    }

    @Test
    fun `eine leere Zelle laesst sich nicht verschieben`() {
        assertNull(TileMove.move(config, "home", 1, 2, "f1"))
    }

    @Test
    fun `volle Ziele stehen gar nicht erst zur Auswahl`() {
        val voll = ordner.copy(
            cells = (0 until 3).flatMap { y -> (0 until 2).map { x -> kachel(x, y, Builtin.CLOCK) } },
        )
        val c = config.copy(screens = listOf(heim, voll))
        assertTrue(TileMove.targetsFor(c, "home", heim.cells.first()).isEmpty())
    }

    // --- Auf demselben Bildschirm: PLAN.md 4.1 "Kacheln tauschen" ---

    @Test
    fun `freie Plaetze und der Nachbar stehen zur Wahl`() {
        val plaetze = TileMove.spotsFor(heim, heim.cells.first())
        // 2x3 minus die eigene Zelle: vier freie Plaetze und der Nachbar rechts.
        assertEquals(5, plaetze.size)
        assertEquals(1, plaetze.count { it.occupant != null })
        assertTrue(plaetze.none { it.x == 0 && it.y == 0 })
    }

    @Test
    fun `auf einen freien Platz ruecken`() {
        val neu = TileMove.moveWithin(config, "home", 0, 0, toX = 1, toY = 2)
        assertNotNull(neu)
        val screen = neu!!.screens.first { it.id == "home" }
        assertNull(screen.cellAt(0, 0))
        assertEquals(
            ButtonAction.Action(Builtin.DIALER),
            screen.cellAt(1, 2)?.button?.action,
        )
        assertEquals(2, screen.cells.size)
    }

    /** Der Kern der Zusage: die andere Kachel bleibt, sie wechselt nur den Platz. */
    @Test
    fun `zwei Kacheln tauschen`() {
        val neu = TileMove.moveWithin(config, "home", 0, 0, toX = 1, toY = 0)
        assertNotNull(neu)
        val screen = neu!!.screens.first { it.id == "home" }
        assertEquals(ButtonAction.Action(Builtin.CAMERA), screen.cellAt(0, 0)?.button?.action)
        assertEquals(ButtonAction.Action(Builtin.DIALER), screen.cellAt(1, 0)?.button?.action)
        assertEquals(2, screen.cells.size)
    }

    /** Ungleich grosse Kacheln liessen beim Tausch ein Loch oder eine Ueberdeckung zurueck. */
    @Test
    fun `ungleich grosse Kacheln tauschen nicht`() {
        val breit = Screen(
            id = "s", name = "S", cols = 2, rows = 3,
            cells = listOf(
                Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
                kachel(0, 1, Builtin.CAMERA),
            ),
        )
        val c = config.copy(screens = listOf(breit))
        assertTrue(TileMove.spotsFor(breit, breit.cells.first()).none { it.occupant != null })
        assertNull(TileMove.moveWithin(c, "s", 0, 0, toX = 0, toY = 1))
    }

    /** Eine breite Kachel behaelt ihre Groesse - und geht nur dorthin, wo sie ganz hinpasst. */
    @Test
    fun `eine breite Kachel passt nicht ueberall hin`() {
        val breit = Screen(
            id = "s", name = "S", cols = 2, rows = 3,
            cells = listOf(
                Cell(0, 0, w = 2, h = 1, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
                kachel(0, 1, Builtin.CAMERA),
            ),
        )
        val plaetze = TileMove.spotsFor(breit, breit.cells.first())
        // Nur die ganz freie letzte Zeile; die mittlere ist halb belegt.
        assertEquals(listOf(0 to 2), plaetze.map { it.x to it.y })
        val c = config.copy(screens = listOf(breit))
        assertNull(TileMove.moveWithin(c, "s", 0, 0, toX = 1, toY = 1))
        val neu = TileMove.moveWithin(c, "s", 0, 0, toX = 0, toY = 2)!!
        val gerueckt = neu.screens.first().cellAt(0, 2)!!
        assertEquals(2, gerueckt.w)
    }

    @Test
    fun `ein Platz ausserhalb des Rasters geht nicht`() {
        assertNull(TileMove.moveWithin(config, "home", 0, 0, toX = 5, toY = 5))
        assertNull(TileMove.moveWithin(config, "home", 0, 0, toX = 0, toY = 0))
    }

    @Test
    fun `von einem leeren Platz aus geht nichts`() {
        assertNull(TileMove.moveWithin(config, "home", 0, 2, toX = 1, toY = 2))
    }
}
