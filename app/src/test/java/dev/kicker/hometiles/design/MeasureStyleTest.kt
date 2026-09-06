package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * measuring happens with the style that is drawn.
 *
 * three places measured with a freshly built `TextStyle` holding only the size. with
 * "Hyperlegible", which is wider than the system font, the measurement said "fits on one
 * line" and it wrapped on the device - the clock tile showed the **2 alone** under
 * "Wednesday, September".
 *
 * the fault is quiet because it only appears with a foreign font: whoever develops with the
 * system font never sees it.
 */
class MeasureStyleTest {

    private fun measuring() = Quelltext.files().filter { "rememberTextMeasurer" in it.readText() }

    @Test
    fun `whoever measures knows the drawn style`() {
        val without = measuring().filterNot { "LocalTextStyle.current" in it.readText() }
        assertEquals("measures without the surface style: $without", emptyList<Any>(), without)
    }

    /** `copy` of the surface style is allowed - then the user's font survives. */
    @Test
    fun `no freshly built style in a measuring file`() {
        val pattern = Regex("""(?:=|to|style =)\s*TextStyle\(""")
        val hits = measuring().flatMap { file ->
            file.readLines().withIndex()
                .filter { pattern.containsMatchIn(it.value) }
                .map { "${file.name}:${it.index + 1}  ${it.value.trim()}" }
        }
        assertEquals("freshly built style instead of copy: $hits", emptyList<String>(), hits)
    }

    /** and they really exist - otherwise the rule checks an empty set. */
    @Test
    fun `measuring files exist`() {
        assertTrue("no file measures text any more", measuring().size >= 3)
    }

    /**
     * `Text(style = ...)` **replaces** the surrounding style, it does not add to it. a
     * `val TabularDigits = TextStyle(fontFeatureSettings = "tnum")` threw the chosen font
     * away: clock, battery level, dial pad and call duration stood in the system font,
     * everything beside them in the user's.
     */
    @Test
    fun `no style kept in stock beside the surface style`() {
        val pattern = Regex("""^(?:internal |private )?val \w+\s*(?::\s*TextStyle\s*)?= TextStyle\(""")
        val hits = Quelltext.files().flatMap { file ->
            file.readLines().withIndex()
                .filter { pattern.containsMatchIn(it.value) }
                .map { "${file.name}:${it.index + 1}  ${it.value.trim()}" }
        }
        assertEquals("style without the surface font: $hits", emptyList<String>(), hits)
    }
}
