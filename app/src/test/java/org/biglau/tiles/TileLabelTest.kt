package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.4: automatische Beschriftung neu angelegter Kacheln.
 *
 * Abgeleitet statt gespeichert - deshalb steht hier auch, was passiert, wenn sich das
 * Ziel danach aendert.
 */
class TileLabelTest {

    private val worte = TileLabel.Words(
        emptyTile = "Leer",
        folder = "Ordner",
        nextScreen = "Nächster Bildschirm",
        widget = "Widget",
    )

    private val screens = mapOf("mehr" to "Mehr", "zwei" to "Zweiter")

    private fun label(button: Button, apps: (ButtonAction.App) -> String? = { "App" }) =
        TileLabel.of(
            button,
            worte,
            screenName = { screens[it] },
            appLabel = apps,
            builtinLabel = { it.name },
        )

    @Test
    fun `eine eigene beschriftung schlaegt alles`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.CAMERA), label = "Oma")
        assertEquals("Oma", label(kachel))
    }

    @Test
    fun `eine app heisst wie die app`() {
        assertEquals("Signal", label(Button(action = ButtonAction.App("org.thoughtcrime", "Main"))) { "Signal" })
    }

    // Ohne installierte App bleibt der Paketname. Haesslich, aber wahr - und besser als
    // eine leere Kachel, auf der nichts steht.
    @Test
    fun `eine fehlende app zeigt das paket`() {
        val kachel = Button(action = ButtonAction.App("org.weg", "Main"))
        assertEquals("org.weg", label(kachel) { null })
    }

    // Der Fall, der die zwei getrennten Ableitungen auffliegen liess: der Editor sagte
    // nur "Ordner". Bei zwei Ordnern war dort nicht mehr zu erkennen, welcher gemeint war.
    @Test
    fun `ein ordner heisst wie der ordner`() {
        assertEquals("Mehr", label(Button(action = ButtonAction.Folder("mehr"))))
    }

    @Test
    fun `ein sprung heisst wie das ziel`() {
        assertEquals("Zweiter", label(Button(action = ButtonAction.GoToScreen("zwei"))))
    }

    // Zeigt der Screen nicht mehr, faellt die Kachel auf ein allgemeines Wort zurueck
    // statt auf eine leere Beschriftung.
    @Test
    fun `ein verschwundenes ziel bleibt benannt`() {
        assertEquals("Ordner", label(Button(action = ButtonAction.Folder("weg"))))
        assertEquals("Nächster Bildschirm", label(Button(action = ButtonAction.GoToScreen("weg"))))
    }

    @Test
    fun `ein kontakt heisst wie der kontakt`() {
        val kachel = Button(action = ButtonAction.Contact("Oma", "0", mode = ContactMode.ASK))
        assertEquals("Oma", label(kachel))
    }

    @Test
    fun `ein widget ohne namen bekommt ein wort`() {
        assertEquals("Widget", label(Button(action = ButtonAction.Widget("com.x/W", 7, ""))))
        assertEquals("Uhr", label(Button(action = ButtonAction.Widget("com.x/W", 7, "Uhr"))))
    }

    @Test
    fun `ein link heisst wie die seite`() {
        assertEquals("orf.at", label(Button(action = ButtonAction.Link("https://orf.at/news"))))
    }

    @Test
    fun `eine leere kachel sagt, dass sie leer ist`() {
        assertEquals("Leer", label(Button()))
    }

    @Test
    fun `eine eingebaute funktion heisst wie die funktion`() {
        assertEquals("CAMERA", label(Button(action = ButtonAction.Action(Builtin.CAMERA))))
    }

    // Jede Aktionsart muss etwas ergeben. Waechst die Liste, faellt das hier auf, bevor
    // eine Kachel ohne Beschriftung auf dem Startbildschirm landet.
    @Test
    fun `keine aktionsart bleibt ohne beschriftung`() {
        val alle = listOf(
            ButtonAction.None,
            ButtonAction.Action(Builtin.CAMERA),
            ButtonAction.App("a", "b"),
            ButtonAction.Contact("Oma", "0", mode = ContactMode.ASK),
            ButtonAction.Shortcut("p", "u", "Kurz"),
            ButtonAction.Widget("com.x/W", 1, "W"),
            ButtonAction.GoToScreen("mehr"),
            ButtonAction.Folder("mehr"),
            ButtonAction.Link("orf.at"),
        )
        val ohne = alle.filter { label(Button(action = it)).isBlank() }
        assertEquals(emptyList<ButtonAction>(), ohne)
    }
}
