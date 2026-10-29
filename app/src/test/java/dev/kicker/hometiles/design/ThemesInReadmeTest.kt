package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import java.io.File
import dev.kicker.hometiles.data.ThemeName
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
    fun `there are still five themes`() {
        assertEquals(
            "the number of themes has changed. a sentence in the README lists them " +
                "(section Themes) - it wants pulling along.",
            5,
            ThemeName.entries.size,
        )
    }

    @Test
    fun `the section names all five`() {
        val place = Quelltext.cut(readme, "## Themes", "\n## ")
        listOf("Dark", "Light", "High contrast", "Paper", "follows the phone").forEach { word ->
            assertTrue("the section Themes is missing: $word", word in place)
        }
    }

    /**
     * every theme a person can pick is shown, not described. "follow the phone" is dark or
     * light, so it has no picture of its own. asked for on 29.10.2026.
     */
    @Test
    fun `every theme has a picture`() {
        val place = Quelltext.cut(readme, "## Themes", "\n## ")
        val missing = ThemeName.entries.filter { it != ThemeName.SYSTEM }.mapNotNull { theme ->
            val path = "docs/themes/" + theme.name.lowercase().replace('_', '-') + ".png"
            path.takeUnless { it in place && File("../$it").isFile }
        }
        assertEquals("these themes have no picture in the README", emptyList<String>(), missing)
    }
}
