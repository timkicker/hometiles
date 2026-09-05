package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * shrink first, then hyphenate or cut.
 *
 * at 200 percent text size one read words split mid-way on the tile, in headings and in the
 * list. compose splits a word right through as soon as it no longer fits the line on its own
 * - and whoever sets 200 percent has gained nothing from a torn word.
 *
 * all three places measure now and step down in size before they split: the tile measures the
 * whole text, heading and row measure the **longest word**, because that is where the line
 * breaks.
 */
class ShrinkBeforeBreakTest {

    private fun source(path: String) = Quelltext.file(path).readText()

    @Test
    fun `tile, heading and row use the same ladder`() {
        listOf(
            "org/biglau/ui/BigTile.kt",
            "org/biglau/ui/BigRow.kt",
        ).forEach { path ->
            assertTrue("$path does not measure in steps", "labelLadder(" in source(path))
        }
    }

    @Test
    fun `heading and row measure the longest word`() {
        val text = source("org/biglau/ui/BigRow.kt")
        // both stand in the same file: BigRow and BigHeading.
        assertTrue("the longest word is not measured", "longestWord(" in text)
        assertTrue(
            "it is measured only once - one of the two places is missing",
            text.split("longestWord(").size - 1 >= 2,
        )
    }

    /**
     * three lines where three lines have room - **measured**, not guessed from the kind of
     * layout. the first attempt only asked whether the height was bounded at all; on the sos
     * screen it is (fixed layout) but generously, so a label kept being cut there while half
     * the screen stood empty.
     */
    @Test
    fun `the third line hangs on the measured height`() {
        val text = source("org/biglau/ui/BigRow.kt")
        assertTrue("the height is not measured", "maxLines = 3" in text)
        assertTrue(
            "it is not checked against the available room",
            "constraints.maxHeight" in text,
        )
    }
}
