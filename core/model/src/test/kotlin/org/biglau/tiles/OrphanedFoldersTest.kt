package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ordner, auf die keine Kachel mehr zeigt.
 *
 * `ScreenEdits.unreachable` lässt Ordner ausdrücklich aus und verweist im Kommentar auf
 * `FolderEdits.orphaned` — „ob eine fehlt, prüft FolderEdits.orphaned". Diese Funktion rief
 * bis hierher **niemand** auf. Zugleich stehen Ordner nicht in der Screen-Liste („Ordner
 * gehören ihrer Kachel"). Ein Ordner ohne Kachel war damit weder zu öffnen noch zu sehen
 * noch zu löschen — seine Kacheln zählten aber weiter beim Zurücksetzen mit und wanderten
 * in jede Sicherung.
 */
class OrphanedFoldersTest {

    private fun cell(x: Int, y: Int, action: ButtonAction) =
        Cell(x = x, y = y, button = Button(action = action))

    /** Startbildschirm mit Ordnerkachel, ein zweiter Screen, der Ordner selbst. */
    private fun mitOrdner(ordnerAuf: String): LauncherConfig {
        val basis = LauncherConfig()
        val home = basis.screens.first().copy(
            id = "home",
            cells = listOf(cell(0, 0, ButtonAction.GoToScreen("s2"))) +
                if (ordnerAuf == "home") listOf(cell(1, 0, ButtonAction.Folder("f1"))) else emptyList(),
        )
        val zweiter = home.copy(
            id = "s2",
            name = "Screen 2",
            cells = if (ordnerAuf == "s2") listOf(cell(0, 0, ButtonAction.Folder("f1"))) else emptyList(),
        )
        val ordner = home.copy(
            id = "f1",
            name = "Mehr",
            kind = ScreenKind.FOLDER,
            cells = listOf(cell(0, 0, ButtonAction.GoToScreen("home"))),
        )
        return basis.copy(screens = listOf(home, zweiter, ordner), homeScreenId = "home")
    }

    private fun ordnerIn(config: LauncherConfig): Screen? =
        config.screens.firstOrNull { it.id == "f1" }

    @Test
    fun `ein Ordner ohne Kachel gilt als verwaist`() {
        val config = mitOrdner("home")
        assertEquals(emptyList<Screen>(), FolderEdits.orphaned(config))

        val ohneKachel = config.copy(
            screens = config.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { c -> c.button.action is ButtonAction.Folder }) else it
            },
        )
        assertEquals(listOf("f1"), FolderEdits.orphaned(ohneKachel).map { it.id })
    }

    @Test
    fun `der Screen-Hinweis meldet Ordner nicht - dafuer ist orphaned da`() {
        // Festgehalten, damit die Arbeitsteilung nicht unbemerkt kippt: der eine Hinweis
        // ist fuer Screens zustaendig, der andere fuer Ordner.
        val ohneKachel = mitOrdner("home").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { x -> x.button.action is ButtonAction.Folder }) else it
            })
        }
        assertTrue("f1" !in ScreenEdits.unreachable(ohneKachel).map { it.id })
        assertEquals(listOf("f1"), FolderEdits.orphaned(ohneKachel).map { it.id })
    }

    @Test
    fun `einen Screen zu loeschen nimmt seine Ordner mit`() {
        // Der Ordner liegt auf "Screen 2". Wird der geloescht, war der Ordner bis hierher
        // fuer immer in der Konfiguration gefangen.
        val nachher = ScreenEdits.delete(mitOrdner("s2"), "s2")
        assertNull("der Ordner muss mit weg sein", ordnerIn(nachher))
        assertEquals(emptyList<Screen>(), FolderEdits.orphaned(nachher))
    }

    @Test
    fun `ein Ordner auf einem anderen Screen bleibt stehen`() {
        // Geloescht wird "Screen 2", der Ordner haengt am Startbildschirm - er geht
        // niemanden etwas an.
        val nachher = ScreenEdits.delete(mitOrdner("home"), "s2")
        assertEquals("Mehr", ordnerIn(nachher)?.name)
    }

    @Test
    fun `die Rueckfrage zaehlt Kacheln und Ordner`() {
        // Der Ordnerinhalt ist zugeklappt - was er kostet, muss dastehen, bevor getippt
        // wird. "Screen 2" traegt genau eine Kachel, und die ist ein Ordner.
        assertEquals(1 to 1, ScreenEdits.deletionLosses(mitOrdner("s2"), "s2"))
    }

    @Test
    fun `ein Ordner, auf den noch anderswo eine Kachel zeigt, zaehlt nicht mit`() {
        // Zwei Kacheln auf denselben Ordner: das Loeschen des einen Screens laesst ihn
        // stehen, also darf die Rueckfrage ihn auch nicht als Verlust nennen.
        val config = mitOrdner("s2").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells + cell(1, 1, ButtonAction.Folder("f1"))) else it
            })
        }
        assertEquals(1 to 0, ScreenEdits.deletionLosses(config, "s2"))
        assertEquals("Mehr", ordnerIn(ScreenEdits.delete(config, "s2"))?.name)
    }

    @Test
    fun `ein leerer Screen kostet nichts`() {
        // "Screen 2" ist in dieser Aufstellung leer, der Ordner haengt am Startbildschirm.
        assertEquals(0 to 0, ScreenEdits.deletionLosses(mitOrdner("home"), "s2"))
    }

    @Test
    fun `der Startbildschirm meldet Kacheln und seinen Ordner`() {
        // Zwei belegte Kacheln, eine davon der Ordner.
        assertEquals(2 to 1, ScreenEdits.deletionLosses(mitOrdner("home"), "home"))
    }

    @Test
    fun `ein Screen, den es nicht gibt, kostet nichts`() {
        assertEquals(0 to 0, ScreenEdits.deletionLosses(mitOrdner("home"), "gibtsnicht"))
    }

    @Test
    fun `ein schon vorher verwaister Ordner verschwindet nicht nebenbei`() {
        // Sonst raeumte das Loeschen eines unbeteiligten Screens stillschweigend Inhalt weg,
        // von dem gerade gar nicht die Rede war.
        val vorher = mitOrdner("home").let { c ->
            c.copy(screens = c.screens.map {
                if (it.id == "home") it.copy(cells = it.cells.filterNot { x -> x.button.action is ButtonAction.Folder }) else it
            })
        }
        val nachher = ScreenEdits.delete(vorher, "s2")
        assertEquals("Mehr", ordnerIn(nachher)?.name)
    }
}
