package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what moving changes about a tile stands there beforehand.
 *
 * `TileMove.move` puts a large tile back to one field, otherwise it would stand over the edge
 * at the target - right, and long checked. unchecked was that anyone learns of it: one tapped
 * and the 2x1 tile stood over there as a square. afterwards that looks like a fault,
 * beforehand it is a condition.
 *
 * the rule hangs on the place in the model that shrinks. if the shrinking goes, the notice
 * may go as well - and the first test falls over and says so.
 */
class ShrunkTileTest {

    private val model = Quelltext.withoutComments("org/biglau/tiles/TileMove.kt")
    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `the model still shrinks the tile on the move`() {
        val move = Quelltext.cut(model, "fun move(")
        assertTrue(
            "TileMove.move no longer shrinks - then the notice in the move view may go, and " +
                "this rule with it.",
            move.contains("w = 1") && move.contains("h = 1"),
        )
    }

    @Test
    fun `the move view says it before it happens`() {
        val view = Quelltext.cut(editor, "fun MoveTargetList(")
        assertTrue(
            "the move view does not mention the shrinking. a tile that silently gets smaller " +
                "on the move looks like a fault.",
            view.contains("move_shrinks"),
        )
    }

    @Test
    fun `the notice comes only for a large tile`() {
        val condition = Quelltext.cut(editor, "shrinks = ", "\n")
        assertTrue(
            "the condition for the notice does not read the tile's size but stands on " +
                "$condition - then it would show for a tile that cannot shrink at all.",
            condition.contains(".w >") && condition.contains(".h >"),
        )
    }

    @Test
    fun `the notice stands in both languages and points a way`() {
        for (file in Quelltext.texts("values") + Quelltext.texts("values-de")) {
            val text = file.readText()
            if (!text.contains("name=\"move_shrinks\"")) continue
            val sentence = Quelltext.cut(text, "name=\"move_shrinks\">", "</string>")
            assertTrue(
                "the notice in ${file.path} names only the problem: $sentence",
                sentence.count { it == '.' } >= 2,
            )
        }
        val found = (Quelltext.texts("values") + Quelltext.texts("values-de"))
            .count { it.readText().contains("name=\"move_shrinks\"") }
        assertTrue("the notice is missing in one of the two languages.", found == 2)
    }
}
