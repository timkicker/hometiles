package dev.kicker.hometiles.apps

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.search.TextSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * the settings row at the end of the app list must be findable by searching too.
 *
 * on the user's device it was on 3.9.2026 the only way into the HomeTiles settings, and it stood
 * there only while the search field was empty.
 */
class SettingsFindableTest {

    private fun value(name: String): String = Quelltext.textValue(name, "values")

    @Test
    fun `the row no longer hangs on an empty search field`() {
        val source = Quelltext.file("dev/kicker/hometiles/apps/AppDrawerActivity.kt").readText()
        val spot = Quelltext.cut(source, "apps_open_settings")
        assertEquals(
            "the settings row must not hang on `query.isEmpty()` again - it is the last way " +
                "there, and whoever searches is searching.",
            false,
            "if (query.isEmpty())" in Quelltext.cut(source, "", "apps_open_settings").takeLast(400),
        )
        assertEquals(true, "TextSearch.rank(settingsLabel" in source || "TextSearch.rank(\n" in spot)
    }

    /** the search word has to be the one of that language - the first try looked for the german word in the english text. */
    @Test
    fun `whoever searches for the settings finds them`() {
        listOf("values" to "setting", "values-de" to "einstell").forEach { (language, word) ->
            val label = Quelltext.textValue("apps_open_settings", language)
            assertNotNull("$language: $label is not hit by $word", TextSearch.rank(label, word))
            assertNotNull("$language: $label is not hit by hometiles", TextSearch.rank(label, "hometiles"))
        }
    }

    @Test
    fun `an empty search field still hits everything`() {
        assertNotNull(TextSearch.rank(value("apps_open_settings"), ""))
        assertNull(TextSearch.rank(value("apps_open_settings"), "zzzz"))
    }
}
