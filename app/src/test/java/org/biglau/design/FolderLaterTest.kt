package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a cancelled step leaves nothing behind.
 *
 * choosing "create folder" on a **folder** tile brings a confirmation: the old folder and
 * its content would be lost. answering "keep" should leave everything as it was.
 *
 * until 04.09.2026 it did not. `onNewFolder` created the new folder screen **before** the
 * confirmation came; saying "keep" left it lying there - empty, unreachable, in every
 * backup. produced on the device and seen in `config.json`: `folder4`, zero tiles, no way to
 * it.
 *
 * it showed only because the settings listed it afterwards among the folders no tile leads
 * to. the comment above that list claimed at the same time that no new ones arise any more -
 * it described the intention, not the state.
 *
 * the rule: the creating hangs on the same condition as the writing.
 */
class FolderLaterTest {

    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `the folder comes into being only on writing`() {
        val from = editor.indexOf("onNewFolder = {")
        assertTrue("onNewFolder no longer exists", from > 0)
        val body = editor.substring(from, minOf(editor.length, from + 400))
        assertTrue(
            "onNewFolder creates the folder itself. then it stays behind when the " +
                "confirmation is answered with keep.",
            "FolderEdits.newFolder" !in body,
        )

        val whileWriting = editor.indexOf("fun writeNow(")
        assertTrue("writeNow no longer exists", whileWriting > 0)
        val writing = editor.substring(whileWriting, minOf(editor.length, whileWriting + 800))
        assertTrue(
            "nobody creates the folder when the tile is written - then the tile points at " +
                "a folder that does not exist.",
            "FolderEdits.newFolder" in writing,
        )
    }

    /** and only if it does not exist yet - otherwise every write would be a new folder. */
    @Test
    fun `an existing folder is not created twice`() {
        val from = editor.indexOf("fun writeNow(")
        val body = editor.substring(from, minOf(editor.length, from + 800))
        assertTrue(
            "the creating does not ask whether the folder is already there.",
            "screens.none" in body,
        )
    }

    /** and the confirmation itself still exists; it counts what would be lost. */
    @Test
    fun `replacing a folder tile asks first`() {
        val from = editor.indexOf("fun write(")
        assertTrue("write no longer exists", from > 0)
        val body = editor.substring(from, minOf(editor.length, from + 300))
        assertTrue(
            "a folder tile is overwritten without a confirmation again - then the folder " +
                "and its content are gone without anyone having been asked.",
            "replacingFolder" in body,
        )
    }
}
