package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * every action must fit on the long press too.
 *
 * `PLAN.md` 4.3: every action additionally assignable to the **long press**, independent of
 * the short press. only apps and builtins were on offer, though the model has long carried
 * contacts, shortcuts, screens and web pages and the home screen runs them. a half-kept
 * promise looks from outside like a setting that does not exist.
 *
 * the price is a case distinction while writing - and that is what the second test checks:
 * were a branch to write straight onto the main action again, a choice made on the long
 * press path would silently overwrite what the tile did before.
 */
class LongPressReachTest {

    private val source = Quelltext.file("dev/kicker/hometiles/tiles/TileEditorActivity.kt").readText()

    @Test
    fun `every kind can be put on the long press as well`() {
        val offered = Regex("""onPick\(Mode\.(\w+)\)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            setOf(
                "PICK_APP", "PICK_CONTACT", "PICK_BUILTIN", "PICK_SHORTCUT_APP", "PICK_SCREEN",
                "EDIT_LINK", "EDIT_NUMBER",
            ),
            offered,
        )
    }

    /**
     * widget and folder are deliberately not on offer: both are not a handle but the content
     * of a cell. they therefore keep writing straight - and only they.
     */
    @Test
    fun `the choice never writes past the case distinction`() {
        val lines = source.lines()
        val strays = lines.mapIndexedNotNull { index, line ->
            if (!line.contains("TileEdits.withAction(")) {
                null
            } else {
                val around = lines.subList(index, minOf(index + 4, lines.size)).joinToString(" ")
                val allowed = "ButtonAction.Widget" in around ||
                    "ButtonAction.Folder" in around ||
                    "forLongPress" in lines.subList(maxOf(0, index - 4), index).joinToString(" ")
                if (allowed) null else "line ${index + 1}: ${line.trim()}"
            }
        }
        assertEquals(emptyList<String>(), strays)
    }
}
