package dev.kicker.hometiles.settings

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.ScreenBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * five colours, five names - and the names arrive.
 *
 * the choice of screen background shows every colour full width, and every row is called the
 * same. whoever does **not** see the colour had five identical offers until 04.09.2026 - read
 * off the node dump on the emulator, five times the same text without any description.
 *
 * two things have to hold for the spoken name: there must be a name for every colour offered,
 * and the names must differ - a list saying "colour" five times would be the same problem
 * with more work.
 */
class ColourNamesTest {

    private val keys = listOf(
        "screen_background_blue",
        "screen_background_violet",
        "screen_background_green",
        "screen_background_red",
        "screen_background_ochre",
    )

    @Test
    fun `there is a name for every colour offered`() {
        val colours = ScreenBackground.choicesFor(ThemeName.DARK, false)
        assertEquals(
            "as many colours are offered, as many names there must be",
            colours.size,
            BACKGROUND_NAMES.size,
        )
        assertEquals(
            "the list in the source and this rule's keys must match",
            keys.size,
            BACKGROUND_NAMES.size,
        )
    }

    @Test
    fun `every name stands in both languages and is unique`() {
        listOf("values-de", "values").forEach { language ->
            val names = keys.map { Quelltext.textValue(it, language) }
            names.forEach { name ->
                assertTrue("a colour name is empty (language \"$language\")", name.isNotBlank())
            }
            assertEquals(
                "two colours have the same name (language \"$language\"): $names",
                names.size,
                names.toSet().size,
            )
        }
    }

    @Test
    fun `the colour row passes the spoken name on`() {
        val list = Quelltext.cut(
            Quelltext.withoutComments("dev/kicker/hometiles/settings/SettingsActivity.kt"),
            from = "itemsIndexed(backgroundColours)",
            to = "\n        }",
        )
        assertTrue(
            "the colour rows do not say their name: $list",
            "labelSpeech" in list && "BACKGROUND_NAMES" in list,
        )
    }
}
