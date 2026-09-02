package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileEditsTest {

    @Test
    fun `Auto-Farbe laeuft zeilenweise durch die Palette`() {
        val indices = (0 until 3).flatMap { y ->
            (0 until 2).map { x -> TileEdits.autoColorIndex(x, y, cols = 2, paletteSize = 6) }
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), indices)
    }

    @Test
    fun `Auto-Farbe beachtet die tatsaechliche Spaltenzahl`() {
        // Der Fehler, den dieser Test festhaelt: mit fest verdrahteten zwei Spalten
        // bekamen in einem Dreierraster benachbarte Kacheln dieselbe Farbe.
        val firstRow = (0 until 3).map { x -> TileEdits.autoColorIndex(x, 0, cols = 3, paletteSize = 6) }
        val secondRow = (0 until 3).map { x -> TileEdits.autoColorIndex(x, 1, cols = 3, paletteSize = 6) }
        assertEquals(listOf(0, 1, 2), firstRow)
        assertEquals(listOf(3, 4, 5), secondRow)
        assertEquals("Innerhalb einer Zeile darf sich keine Farbe wiederholen", 3, firstRow.toSet().size)
    }

    @Test
    fun `Auto-Farbe laeuft bei grossen Rastern rundum weiter`() {
        val index = TileEdits.autoColorIndex(x = 5, y = 7, cols = 6, paletteSize = 6)
        assertTrue(index in 0..5)
        assertEquals((7 * 6 + 5) % 6, index)
    }

    @Test
    fun `Auto-Farbe lehnt unsinnige Raster ab`() {
        listOf(0 to 6, 3 to 0, -1 to 6).forEach { (cols, palette) ->
            try {
                TileEdits.autoColorIndex(0, 0, cols, palette)
                throw AssertionError("cols=$cols palette=$palette haette abgelehnt werden muessen")
            } catch (expected: IllegalArgumentException) {
                // so soll es sein
            }
        }
    }

    @Test
    fun `eine eigene Beschriftung ueberlebt den Wechsel innerhalb derselben Art`() {
        val button = Button(ButtonAction.App("a.b", "a.b.Main"), label = "Oma")
        assertEquals("Oma", TileEdits.withAction(button, ButtonAction.App("c.d", "c.d.Main")).label)
    }

    @Test
    fun `eine eigene Beschriftung faellt beim Wechsel der Art weg`() {
        // "Oma" auf einer Kamera-Kachel waere schlimmer als gar keine Beschriftung.
        val button = Button(ButtonAction.Contact("Oma", "+43123"), label = "Oma")
        assertNull(TileEdits.withAction(button, ButtonAction.Action(Builtin.CAMERA)).label)
    }

    @Test
    fun `ohne eigene Beschriftung aendert sich beim Wechsel nichts daran`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER))
        val next = TileEdits.withAction(button, ButtonAction.Action(Builtin.CAMERA))
        assertNull(next.label)
        assertEquals(ButtonAction.Action(Builtin.CAMERA), next.action)
    }

    @Test
    fun `leere Eingabe heisst automatisch beschriften`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), label = "Anrufen")
        assertNull(TileEdits.withLabel(button, "").label)
        assertNull(TileEdits.withLabel(button, "   ").label)
        assertNull(TileEdits.withLabel(button, null).label)
    }

    @Test
    fun `Beschriftungen werden getrimmt`() {
        assertEquals("Oma", TileEdits.withLabel(Button(), "  Oma  ").label)
    }

    @Test
    fun `ein freier Farbton loescht die Palettenwahl`() {
        // Sonst gaebe es zwei Antworten auf dieselbe Frage, und welche gilt, haenge an der
        // Reihenfolge im Zeichencode.
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorIndex = 3)
        val next = TileEdits.withColorHue(button, 210f)
        assertEquals(210f, next.colorHue)
        assertEquals(-1, next.colorIndex)
    }

    @Test
    fun `eine Farbe aus der Palette loescht den freien Ton`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorHue = 210f)
        val next = TileEdits.withColorIndex(button, 3)
        assertEquals(3, next.colorIndex)
        assertNull(next.colorHue)
    }

    @Test
    fun `Farbe automatisch loescht auch den freien Ton`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorHue = 210f)
        val next = TileEdits.withColorIndex(button, null)
        assertEquals(-1, next.colorIndex)
        assertNull(next.colorHue)
    }

    @Test
    fun `Farbe automatisch setzt den Index zurueck`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorIndex = 4)
        assertEquals(-1, TileEdits.withColorIndex(button, null).colorIndex)
    }

    @Test
    fun `Leeren entfernt wirklich alles`() {
        val button = Button(
            action = ButtonAction.Contact("Oma", "+43123", mode = ContactMode.SMS),
            label = "Oma",
            colorIndex = 2,
            longPress = ButtonAction.Action(Builtin.SOS),
        )
        val cleared = Button()
        assertEquals(ButtonAction.None, cleared.action)
        assertNull(cleared.label)
        assertEquals(-1, cleared.colorIndex)
        assertNull(cleared.longPress)
        assertEquals("Die Ausgangskachel darf nicht veraendert werden", "Oma", button.label)
    }

    @Test
    fun `eine Langdruck-Aktion ohne Wirkung wird nicht gespeichert`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER))
        assertNull(TileEdits.withLongPress(button, ButtonAction.None).longPress)
        assertEquals(
            ButtonAction.Action(Builtin.SOS),
            TileEdits.withLongPress(button, ButtonAction.Action(Builtin.SOS)).longPress,
        )
    }

    // Die Zweitbelegung darf jetzt auch eine App sein - "Halten oeffnet Spotify" ist
    // der Fall, den man wirklich will, nicht nur eine eingebaute Funktion.
    @Test
    fun `zweitbelegung nimmt eine app`() {
        val kachel = Button(action = ButtonAction.App("org.example", "org.example.Main"))
        val mit = TileEdits.withLongPress(kachel, ButtonAction.App("com.spotify.music", "Main"))
        assertEquals(ButtonAction.App("com.spotify.music", "Main"), mit.longPress)
        assertEquals(kachel.action, mit.action)
    }

    // Eine Zweitbelegung, die nichts tut, waere schlimmer als keine: das Halten
    // faende dann weder eine Aktion noch den Editor.
    @Test
    fun `leere zweitbelegung wird nicht gespeichert`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.CAMERA))
        assertNull(TileEdits.withLongPress(kachel, ButtonAction.None).longPress)
        assertNull(TileEdits.withLongPress(kachel, null).longPress)
    }
}
