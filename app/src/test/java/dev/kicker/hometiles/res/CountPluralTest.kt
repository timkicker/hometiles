package dev.kicker.hometiles.res

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a text that counts something is a plural.
 *
 * "1 entries deleted" in the call log, found on screen; two rounds later "Costs 1 text
 * messages" under the message field. finding the same fault by hand a second time is once
 * too often.
 *
 * not every number counts: measures ("2 x 3 cells"), positions ("step 2 of 5") and names
 * ("Screen 3") carry no plural. those stand below with a reason - an exception without one is
 * only a fault turned down quieter.
 */
class CountPluralTest {

    /**
     * **all** text files and **both** languages. `.first()` was the same thing while only
     * `:app` had texts, and the fault that started this was a german sentence.
     */
    private val strings = Quelltext.texts("values") + Quelltext.texts("values-de")

    /** numbers that count nothing. */
    private val doesNotCount = mapOf(
        "resize_current" to "measure: columns x rows",
        "screen_grid" to "measure: columns x rows",
        "screen_is_home" to "measure: columns x rows",
        "widget_needs" to "measure: columns x rows",
        "widget_fixed" to "measure: columns x rows",
        "widget_no_room" to "measure: columns x rows",
        "screen_default_name" to "name: Screen 1, Screen 2",
        "swipe_order_position" to "position: place 2 of 5",
        "wizard_position" to "position: step 2 of 5",
        "sos_numbers_hint" to "an upper bound, never one: up to 5 people",
        "move_spot" to "position in the grid: row 2, column 1",
        "editor_where" to "position in the grid, with the screen first: home, row 4, column 1",
        "move_which" to "position in the grid, with the tile first: contacts, row 2, column 1",
        "a11y_signal_bars" to "measure: filled of four steps, like 2 x 3 cells",
    )

    private fun withNumber(): List<String> =
        Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""")
            .findAll(strings.joinToString("\n") { it.readText() })
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `every counting text is a plural`() {
        val unchecked = withNumber().filterNot { it in doesNotCount }
        assertEquals(
            "these texts count something and still stand as a single string - at the number " +
                "1 they read wrong: $unchecked",
            emptyList<String>(),
            unchecked,
        )
    }

    @Test
    fun `every exception really exists and names its reason`() {
        val present = withNumber().toSet()
        doesNotCount.forEach { (name, reason) ->
            assertTrue("$name no longer exists - strike the exception", name in present)
            assertTrue("$name needs a reason", reason.length > 10)
        }
    }

    /**
     * a plural whose singular and plural are the same sentence is none: the form is right,
     * `TranslationsTest` is satisfied, and the screen still says "1 entries".
     *
     * this assumes every language inflects the counted word. italian came close - `app` is
     * invariable there - and was solved with `applicazione`/`applicazioni` rather than by
     * touching the rule. a language where that cannot be worked around belongs here as an
     * exception with a reason, not as a softening.
     */
    @Test
    fun `no plural says the same thing twice`() {
        val same = Quelltext.allTexts()
            .filter { it.name == "plurals.xml" }
            .flatMap { file ->
                Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(file.readText())
                    .mapNotNull { hit ->
                        val forms = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                            .findAll(hit.groupValues[2])
                            .associate { it.groupValues[1] to it.groupValues[2].trim() }
                        val one = forms["one"]
                        val other = forms["other"]
                        if (one != null && one == other) {
                            file.parentFile.name + "/" + hit.groupValues[1] + ": " + one
                        } else {
                            null
                        }
                    }
            }
        assertEquals(
            "singular and plural are the same sentence here - then the plural form is only " +
                "form and the text still reads wrong at 1",
            emptyList<String>(),
            same,
        )
    }

    @Test
    fun `the rule would find a counting text`() {
        // counter-check: without it a broken pattern would wave everything through.
        val probe = """<string name="test_counter">%1${'$'}d entries</string>"""
        val hit = Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""").find(probe)
        assertTrue("a counting text has to stand out", hit != null)

        val doubled = """<plurals name="test"><item quantity="one">%1${'$'}d entries</item><item quantity="other">%1${'$'}d entries</item></plurals>"""
        val forms = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(doubled)
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
        assertEquals("the counter-check has to see two equal forms", forms["one"], forms["other"])
    }
}
