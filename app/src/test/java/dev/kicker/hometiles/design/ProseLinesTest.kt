package dev.kicker.hometiles.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a paragraph in the readme is one line.
 *
 * both readmes were wrapped at about 95 columns. github hides that, f-droid did not: the store
 * texts broke in the middle of every sentence until 28.10.2026, see StoreTextsTest. and a
 * wrapped file teaches the next paragraph to be wrapped too, which happened on 29.10.2026.
 *
 * code blocks, tables, html and headings may stand on consecutive lines. prose may not.
 */
class ProseLinesTest {

    private val files = listOf(File("../README.md"), File("../tools/README.md"))

    private val block = Regex("""^\s*([-*+]|\d+\.)\s|^\s*[#|<>!]|^\s*```""")

    private fun wrapped(name: String, text: String): List<String> {
        var fence = false
        return text.split("\n").zipWithNext().mapIndexedNotNull { index, (one, next) ->
            if (one.trimStart().startsWith("```")) fence = !fence
            val prose = next.isNotBlank() && !block.containsMatchIn(next)
            val after = one.isNotBlank() && !Regex("""^\s*[#|<]""").containsMatchIn(one)
            if (!fence && prose && after) "$name:${index + 2} \"${next.trim().take(40)}\"" else null
        }
    }

    @Test
    fun `the readmes carry no wrapped paragraph`() {
        assertTrue("no readme was read", files.all { it.isFile })
        assertEquals(
            "a paragraph stands on one line. these lines continue the one above.",
            emptyList<String>(),
            files.flatMap { wrapped(it.path, it.readText()) },
        )
    }

    @Test
    fun `the rule finds a wrapped paragraph and a wrapped list item`() {
        // counter-check with the shape both readmes had before 29.10.2026.
        val old = "Some phones ship an app blocker\nof their own.\n\n- A list item that\n  goes on.\n"
        assertEquals(2, wrapped("old", old).size)
        val fine = "# Title\n\nOne line.\n\n- a\n- b\n\n```sh\nline one\nline two\n```\n\n| a |\n| b |\n"
        assertEquals(emptyList<String>(), wrapped("new", fine))
    }
}
