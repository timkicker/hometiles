package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.ContactMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.4: the automatic label of a newly made tile.
 *
 * derived instead of stored - so what happens when the target changes afterwards stands here
 * too.
 */
class TileLabelTest {

    private val words = TileLabel.Words(
        emptyTile = "Empty",
        folder = "Folder",
        nextScreen = "Next screen",
        widget = "Widget",
    )

    private val screens = mapOf("more" to "More", "two" to "Second")

    private fun label(button: Button, apps: (ButtonAction.App) -> String? = { "App" }) =
        TileLabel.of(
            button,
            words,
            screenName = { screens[it] },
            appLabel = apps,
            builtinLabel = { it.name },
        )

    @Test
    fun `an own label beats everything`() {
        val tile = Button(action = ButtonAction.Action(Builtin.CAMERA), label = "Alex")
        assertEquals("Alex", label(tile))
    }

    @Test
    fun `an app is called like the app`() {
        assertEquals("Signal", label(Button(action = ButtonAction.App("org.thoughtcrime", "Main"))) { "Signal" })
    }

    // without the app installed the package name stays. ugly, but true - and better than an
    // empty tile with nothing on it.
    @Test
    fun `a missing app shows the package`() {
        val tile = Button(action = ButtonAction.App("org.gone", "Main"))
        assertEquals("org.gone", label(tile) { null })
    }

    // the case that exposed the two separate derivations: the editor said only "folder". with
    // two folders one could no longer tell which was meant.
    @Test
    fun `a folder is called like the folder`() {
        assertEquals("More", label(Button(action = ButtonAction.Folder("more"))))
    }

    @Test
    fun `a jump is called like its target`() {
        assertEquals("Second", label(Button(action = ButtonAction.GoToScreen("two"))))
    }

    // with the screen gone the tile falls back on a general word instead of an empty label.
    @Test
    fun `a vanished target stays named`() {
        assertEquals("Folder", label(Button(action = ButtonAction.Folder("gone"))))
        assertEquals("Next screen", label(Button(action = ButtonAction.GoToScreen("gone"))))
    }

    @Test
    fun `a contact is called like the contact`() {
        val tile = Button(action = ButtonAction.Contact("Alex", "0", mode = ContactMode.ASK))
        assertEquals("Alex", label(tile))
    }

    @Test
    fun `a widget without a name gets a word`() {
        assertEquals("Widget", label(Button(action = ButtonAction.Widget("com.x/W", 7, ""))))
        assertEquals("Clock", label(Button(action = ButtonAction.Widget("com.x/W", 7, "Clock"))))
    }

    @Test
    fun `a link is called like the site`() {
        assertEquals("orf.at", label(Button(action = ButtonAction.Link("https://orf.at/news"))))
    }

    @Test
    fun `an empty tile says that it is empty`() {
        assertEquals("Empty", label(Button()))
    }

    @Test
    fun `a builtin function is called like the function`() {
        assertEquals("CAMERA", label(Button(action = ButtonAction.Action(Builtin.CAMERA))))
    }

    // every kind of action must yield something. as the list grows this shows up before a
    // tile without a label lands on the home screen.
    @Test
    fun `no kind of action stays without a label`() {
        val all = listOf(
            ButtonAction.None,
            ButtonAction.Action(Builtin.CAMERA),
            ButtonAction.App("a", "b"),
            ButtonAction.Contact("Alex", "0", mode = ContactMode.ASK),
            ButtonAction.Shortcut("p", "u", "Short"),
            ButtonAction.Widget("com.x/W", 1, "W"),
            ButtonAction.GoToScreen("more"),
            ButtonAction.Folder("more"),
            ButtonAction.Link("orf.at"),
        )
        val without = all.filter { label(Button(action = it)).isBlank() }
        assertEquals(emptyList<ButtonAction>(), without)
    }
}
