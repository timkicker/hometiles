package org.biglau

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tile whose app is gone leads to the editor, not only to a sentence.
 *
 * uninstalling an app leaves its tile standing. tapping it said the app was gone and to
 * reassign the tile, and left it at that: how to reassign was the long press, which one has
 * to think of first and which can be switched off - then nothing led there at all.
 *
 * the same rule as with the emergency call without contacts, the call log and the unreachable
 * screen: the way there instead of directions to it.
 */
class DeadTileTest {

    private val source = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    /**
     * `app_gone` stood in this file twice - once on the tap, once in the pin flow of the app
     * lock - and the first version of this rule searched the first occurrence and fell over
     * the wrong one. now both ways go through `startAction`, the cell arrives there as a
     * parameter, and the rule looks for the place the message stands rather than for a
     * function it knows by name.
     */
    private val afterTheMessage = source.substringAfter("R.string.app_gone", "")

    @Test
    fun `after the message about the missing app the editor opens`() {
        assertTrue("the message no longer exists", afterTheMessage.isNotEmpty())
        assertTrue(
            "no way to reassign follows the message - then the user stands in front of a " +
                "sentence again instead of an action.",
            "TileEditorActivity.intent(" in afterTheMessage.take(400),
        )
    }

    /**
     * and the editor has to get **the** cell that was tapped.
     *
     * what the coordinates are called on the way is none of the rule's business - they were
     * `cell.x`/`cell.y` until the start got a function of its own, and the rule fell over
     * although the same cell arrived. so what is asked is the **beginning** of the way:
     * whoever calls the start has to pass the tapped cell.
     */
    @Test
    fun `the editor gets the cell that was tapped`() {
        val calls = Quelltext.file("org/biglau/MainActivity.kt").readLines()
            .filter { it.trim().startsWith("startAction(") }
        assertTrue("nobody calls the start - does the rule still read what it means?", calls.isNotEmpty())
        val withoutCell = calls.filterNot { "cell.x" in it || "pending.x" in it }
        assertTrue(
            "something is started here without passing the tapped cell: $withoutCell - then " +
                "one lands on some tile or other after the message.",
            withoutCell.isEmpty(),
        )
    }
}
