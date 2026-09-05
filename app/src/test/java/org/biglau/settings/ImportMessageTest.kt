package org.biglau.settings

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "could not be opened" and "is no backup" are two different answers.
 *
 * a file BigLau had written itself two minutes earlier was turned away as no backup when
 * opened from outside - in truth it only could not be read (an address the app may not reach).
 * whoever reads that sentence looks for the fault in their file and may throw away the only
 * backup they have.
 */
class ImportMessageTest {

    private val source = Quelltext.file("org/biglau/settings/ImportActivity.kt").readText()

    @Test
    fun `unreadable and unreadable-as-a-backup are two cases`() {
        val block = Quelltext.cut(source, "text = when {", "},")
        assertTrue("the read error is missing: $block", "transfer_unreadable" in block)
        assertTrue("the format error is missing: $block", "transfer_bad_file" in block)
        assertTrue(
            "the read error must be checked first, otherwise the other one covers it",
            block.indexOf("transfer_unreadable") < block.indexOf("transfer_bad_file"),
        )
    }

    @Test
    fun `both sentences stand in both languages`() {
        // per language, not per file: the texts lie in several modules by now, and a sentence
        // belongs in *one* of them, not in each.
        listOf("values", "values-de").forEach { language ->
            val texts = Quelltext.texts(language).joinToString("\n") { it.readText() }
            listOf("transfer_unreadable", "transfer_bad_file").forEach { name ->
                assertTrue("$language: $name is missing", "\"$name\"" in texts)
            }
        }
    }
}
