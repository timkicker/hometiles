package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileEditsTest {

    @Test
    fun `the automatic colour runs through the palette row by row`() {
        val indices = (0 until 3).flatMap { y ->
            (0 until 2).map { x -> TileEdits.autoColorIndex(x, y, cols = 2, paletteSize = 6) }
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), indices)
    }

    @Test
    fun `the automatic colour minds the actual number of columns`() {
        // with two columns wired in, neighbouring tiles in a three-column grid got the same
        // colour.
        val firstRow = (0 until 3).map { x -> TileEdits.autoColorIndex(x, 0, cols = 3, paletteSize = 6) }
        val secondRow = (0 until 3).map { x -> TileEdits.autoColorIndex(x, 1, cols = 3, paletteSize = 6) }
        assertEquals(listOf(0, 1, 2), firstRow)
        assertEquals(listOf(3, 4, 5), secondRow)
        assertEquals("no colour may repeat within one row", 3, firstRow.toSet().size)
    }

    @Test
    fun `the automatic colour wraps around on large grids`() {
        val index = TileEdits.autoColorIndex(x = 5, y = 7, cols = 6, paletteSize = 6)
        assertTrue(index in 0..5)
        assertEquals((7 * 6 + 5) % 6, index)
    }

    @Test
    fun `the automatic colour rejects nonsensical grids`() {
        listOf(0 to 6, 3 to 0, -1 to 6).forEach { (cols, palette) ->
            try {
                TileEdits.autoColorIndex(0, 0, cols, palette)
                throw AssertionError("cols=$cols palette=$palette should have been rejected")
            } catch (expected: IllegalArgumentException) {
                // as it should be
            }
        }
    }

    @Test
    fun `an own label survives a change within the same kind`() {
        val button = Button(ButtonAction.App("a.b", "a.b.Main"), label = "Alex")
        assertEquals("Alex", TileEdits.withAction(button, ButtonAction.App("c.d", "c.d.Main")).label)
    }

    @Test
    fun `an own label falls away when the kind changes`() {
        // a person's name on a camera tile would be worse than no label at all.
        val button = Button(ButtonAction.Contact("Alex", "+43123"), label = "Alex")
        assertNull(TileEdits.withAction(button, ButtonAction.Action(Builtin.CAMERA)).label)
    }

    @Test
    fun `without an own label a change alters nothing about it`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER))
        val next = TileEdits.withAction(button, ButtonAction.Action(Builtin.CAMERA))
        assertNull(next.label)
        assertEquals(ButtonAction.Action(Builtin.CAMERA), next.action)
    }

    @Test
    fun `empty input means label automatically`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), label = "Call")
        assertNull(TileEdits.withLabel(button, "").label)
        assertNull(TileEdits.withLabel(button, "   ").label)
        assertNull(TileEdits.withLabel(button, null).label)
    }

    @Test
    fun `labels are trimmed`() {
        assertEquals("Alex", TileEdits.withLabel(Button(), "  Alex  ").label)
    }

    @Test
    fun `a free hue clears the palette choice`() {
        // otherwise there are two answers to the same question, and which one holds hangs on
        // the order in the drawing code.
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorIndex = 3)
        val next = TileEdits.withColorHue(button, 210f)
        assertEquals(210f, next.colorHue)
        assertEquals(-1, next.colorIndex)
    }

    @Test
    fun `a colour from the palette clears the free hue`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorHue = 210f)
        val next = TileEdits.withColorIndex(button, 3)
        assertEquals(3, next.colorIndex)
        assertNull(next.colorHue)
    }

    @Test
    fun `colour automatically clears the free hue too`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorHue = 210f)
        val next = TileEdits.withColorIndex(button, null)
        assertEquals(-1, next.colorIndex)
        assertNull(next.colorHue)
    }

    @Test
    fun `colour automatically resets the index`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER), colorIndex = 4)
        assertEquals(-1, TileEdits.withColorIndex(button, null).colorIndex)
    }

    @Test
    fun `clearing really removes everything`() {
        val button = Button(
            action = ButtonAction.Contact("Alex", "+43123", mode = ContactMode.SMS),
            label = "Alex",
            colorIndex = 2,
            longPress = ButtonAction.Action(Builtin.SOS),
        )
        val cleared = Button()
        assertEquals(ButtonAction.None, cleared.action)
        assertNull(cleared.label)
        assertEquals(-1, cleared.colorIndex)
        assertNull(cleared.longPress)
        assertEquals("the tile started from must not be changed", "Alex", button.label)
    }

    @Test
    fun `a long-press action without effect is not stored`() {
        val button = Button(ButtonAction.Action(Builtin.DIALER))
        assertNull(TileEdits.withLongPress(button, ButtonAction.None).longPress)
        assertEquals(
            ButtonAction.Action(Builtin.SOS),
            TileEdits.withLongPress(button, ButtonAction.Action(Builtin.SOS)).longPress,
        )
    }

    // the second action may be an app now - holding opens a music player is the case one
    // really wants, not only a builtin function.
    @Test
    fun `the second action takes an app`() {
        val tile = Button(action = ButtonAction.App("org.example", "org.example.Main"))
        val withApp = TileEdits.withLongPress(tile, ButtonAction.App("com.spotify.music", "Main"))
        assertEquals(ButtonAction.App("com.spotify.music", "Main"), withApp.longPress)
        assertEquals(tile.action, withApp.action)
    }

    // a second action that does nothing would be worse than none: holding would then find
    // neither an action nor the editor.
    @Test
    fun `an empty second action is not stored`() {
        val tile = Button(action = ButtonAction.Action(Builtin.CAMERA))
        assertNull(TileEdits.withLongPress(tile, ButtonAction.None).longPress)
        assertNull(TileEdits.withLongPress(tile, null).longPress)
    }
}
