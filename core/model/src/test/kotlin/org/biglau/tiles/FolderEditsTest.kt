package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ordner: anlegen, hineinschauen, löschen.
 *
 * Der gefährliche Fall ist das Löschen. Ein Ordner und die Kachel, die ihn öffnet, gehören
 * zusammen; bleibt eines von beiden zurück, entsteht genau das, wovor die Einstellungen sonst
 * warnen - ein Screen, zu dem kein Weg führt, oder eine Kachel, die ins Leere zeigt.
 */
class FolderEditsTest {

    private fun app(name: String) =
        Button(action = ButtonAction.App(name, "$name.Main"))

    private val ordner = Screen(
        id = "f1",
        name = "Bank",
        cols = 2,
        rows = 3,
        kind = ScreenKind.FOLDER,
        cells = listOf(
            Cell(1, 1, button = app("b")),
            Cell(0, 0, button = app("a")),
            Cell(0, 2, button = app("e")),
            Cell(1, 0, button = app("c")),
            Cell(0, 1, button = app("d")),
        ),
    )

    private val start = Screen(
        id = "home",
        name = "Start",
        cells = listOf(
            Cell(0, 0, button = Button(action = ButtonAction.Action(Builtin.DIALER))),
            Cell(1, 0, button = Button(action = ButtonAction.Folder("f1"))),
        ),
    )

    private val config = LauncherConfig(screens = listOf(start, ordner), homeScreenId = "home")

    @Test
    fun `ein neuer Ordner ist leer und hat keine Heim-Kachel`() {
        // Anders als ein neuer Screen: der Ordner schliesst sich mit der Zurueck-Geste,
        // er kann keine Sackgasse werden.
        val neu = FolderEdits.newFolder("f2", "Ämter", start)
        assertTrue(neu.isFolder)
        assertTrue(neu.cells.isEmpty())
        assertEquals(start.cols, neu.cols)
        assertEquals(start.rows, neu.rows)
    }

    @Test
    fun `die Vorschau zeigt vier Kacheln, von links oben gelesen`() {
        val vorschau = FolderEdits.preview(ordner)
        assertEquals(FolderEdits.PREVIEW_COUNT, vorschau.size)
        assertEquals(listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), vorschau.map { it.x to it.y })
    }

    @Test
    fun `ein leerer Ordner hat eine leere Vorschau`() {
        assertTrue(FolderEdits.preview(FolderEdits.newFolder("f2", "Leer", start)).isEmpty())
    }

    @Test
    fun `die Screen-Verwaltung zeigt Ordner nicht`() {
        assertEquals(listOf("home"), FolderEdits.plainScreens(config).map { it.id })
        assertEquals(listOf("f1"), FolderEdits.folders(config).map { it.id })
    }

    @Test
    fun `in einem Ordner gibt es keinen Ordner`() {
        assertTrue(FolderEdits.mayContainFolder(start))
        assertFalse(FolderEdits.mayContainFolder(ordner))
    }

    @Test
    fun `loeschen nimmt den Ordner und die Kachel mit`() {
        val danach = FolderEdits.delete(config, "f1")
        assertNull(danach.screens.firstOrNull { it.id == "f1" })
        val heim = danach.screens.first { it.id == "home" }
        assertEquals(1, heim.cells.size)
        assertTrue(heim.cells.single().button.action is ButtonAction.Action)
    }

    @Test
    fun `loeschen nennt vorher die Zahl der Kacheln`() {
        assertEquals(5, FolderEdits.contentCount(config, "f1"))
        assertEquals(0, FolderEdits.contentCount(config, "gibtsnicht"))
    }

    @Test
    fun `einen gewoehnlichen Screen loescht das nicht`() {
        // delete ist nur fuer Ordner; ein Screen geht weiter ueber ScreenEdits, samt
        // seiner eigenen Pruefungen.
        assertEquals(config, FolderEdits.delete(config, "home"))
    }

    @Test
    fun `ein Ordner ohne Kachel faellt auf`() {
        val ohneKachel = config.copy(
            screens = listOf(start.copy(cells = start.cells.take(1)), ordner),
        )
        assertEquals(listOf("f1"), FolderEdits.orphaned(ohneKachel).map { it.id })
        assertTrue(FolderEdits.orphaned(config).isEmpty())
    }

    @Test
    fun `die Kachel findet ihren Ordner`() {
        assertEquals("f1", FolderEdits.folderFor(config, ButtonAction.Folder("f1"))?.id)
        assertNull(FolderEdits.folderFor(config, ButtonAction.Folder("gibtsnicht")))
        // Ein gewoehnlicher Screen ist kein Ordner, auch wenn eine Ordnerkachel auf ihn zeigt.
        assertNull(FolderEdits.folderFor(config, ButtonAction.Folder("home")))
    }
}

/**
 * Ein Ordner hat genau einen Namen.
 *
 * Er steht auf der Kachel und als Überschrift im geöffneten Ordner. Gäbe es daneben noch
 * eine Kachelbeschriftung, hieße dasselbe Ding zweimal anders - je nachdem, ob man davor
 * steht oder darin. Umbenannt wird deshalb der Ordner, nicht die Kachel.
 */
class FolderNameTest {

    private val ordner = Screen(id = "f1", name = "Bank", kind = ScreenKind.FOLDER)
    private val heim = Screen(
        id = "home",
        name = "Start",
        cells = listOf(Cell(0, 0, button = Button(action = ButtonAction.Folder("f1")))),
    )
    private val config = LauncherConfig(screens = listOf(heim, ordner), homeScreenId = "home")

    @Test
    fun `umbenennen aendert den Ordner`() {
        val danach = ScreenEdits.rename(config, "f1", "Ämter")
        assertEquals("Ämter", danach.screens.first { it.id == "f1" }.name)
    }

    @Test
    fun `die Kachel bleibt dabei unberuehrt`() {
        // Sie traegt keine eigene Beschriftung, sie zeigt den Namen des Ordners.
        val danach = ScreenEdits.rename(config, "f1", "Ämter")
        val kachel = danach.screens.first { it.id == "home" }.cells.single()
        assertNull(kachel.button.label)
        assertEquals(ButtonAction.Folder("f1"), kachel.button.action)
    }

    @Test
    fun `ein leerer Name aendert nichts`() {
        // Sonst stuende auf der Kachel gar nichts mehr, und im Ordner auch nicht.
        assertEquals("Bank", ScreenEdits.rename(config, "f1", "   ").screens.first { it.id == "f1" }.name)
    }
}
