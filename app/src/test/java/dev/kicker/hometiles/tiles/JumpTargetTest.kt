package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a folder is no jump target.
 *
 * a folder belongs to its tile and lies over it as an overlay - name on top, a "close folder"
 * row at the bottom. as the target of a jump tile it becomes an ordinary screen: without that
 * row, without an entry in the screen list, and beside it still stands the tile that opens it
 * as an overlay. two ways to the same thing that look different.
 *
 * created and looked at on 04.09.2026: the folder stood there as a screen, seven empty slots
 * and a camera, with a header and no way out but the back gesture.
 *
 * `SwipeChain` has always filtered folders out, `ScreenEdits.unreachable` too. the screen
 * where one **picks** the target was the only place that did not - the rule was there, only
 * not everywhere.
 */
class JumpTargetTest {

    @Test
    fun `the choice offers no folder`() {
        val picker = Quelltext.cut(
            Quelltext.withoutComments("dev/kicker/hometiles/tiles/TileEditorActivity.kt"),
            from = "private fun ScreenPicker(",
            to = "\n}",
        )
        assertTrue(
            "the screen picker offers folders as well:\n$picker",
            "!it.isFolder" in picker,
        )
    }

    @Test
    fun `all three lists of screens filter alike`() {
        // wherever screens are listed for the user, folders do not belong. there are three
        // places: swiping, the list of unreachable screens, and picking a jump target.
        val places = mapOf(
            "dev/kicker/hometiles/tiles/SwipeChain.kt" to "swiping",
            "dev/kicker/hometiles/tiles/ScreenEdits.kt" to "the unreachable screens",
            "dev/kicker/hometiles/tiles/TileEditorActivity.kt" to "picking the jump target",
        )
        val without = places.filterKeys { path ->
            "isFolder" !in Quelltext.withoutComments(path)
        }
        assertTrue(
            "this listing of screens does not tell folders apart: " +
                without.values.joinToString(", "),
            without.isEmpty(),
        )
    }
}
