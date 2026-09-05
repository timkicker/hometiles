package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.biglau.data.ThemeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the README counts the themes - and the number ages.
 *
 * until 3.9.2026 it named three while `ThemeName` knew four; the missing one was "follow the
 * phone", exactly the setting one expects and did not find in the text.
 *
 * the rule does not check the wording but keeps a **reminder**: if a theme comes or goes it
 * falls over and points at the sentence in the README. without that a list ages silently -
 * the same class of fault as a checksum without a commit.
 */
class ThemesInReadmeTest {

    private val readme = File("../README.md").readText()

    @Test
    fun `there are still four themes`() {
        assertEquals(
            "the number of themes has changed. a sentence in the README lists them " +
                "(section Looks) - it wants pulling along.",
            4,
            ThemeName.entries.size,
        )
    }

    @Test
    fun `the sentence names all four`() {
        val place = Quelltext.cut(readme, "## Looks", "##")
        listOf("Dark", "light", "contrast", "follow the phone").forEach { word ->
            assertTrue("the section Looks is missing: $word", word in place)
        }
    }
}
