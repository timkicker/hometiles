package org.biglau.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.biglau.apps.LaunchableApp
import org.junit.Test

class TextSearchTest {

    private fun app(label: String) = LaunchableApp(label, "pkg.${label.lowercase()}", "pkg.Main")

    // the german names are the check object: umlauts and eszett are what the folding is for.
    private val apps = listOf(
        app("AnkiDroid"),
        app("Brave"),
        app("Calculator"),
        app("Google Maps"),
        app("Müller App"),
        app("Öffi"),
        app("WhatsApp"),
        app("Photo Editor"),
    )

    @Test
    fun `an empty query leaves the list unchanged`() {
        assertEquals(apps, TextSearch.filter(apps, "") { it.label })
        assertEquals(apps, TextSearch.filter(apps, "   ") { it.label })
    }

    @Test
    fun `the start of the name ranks before a hit in the middle`() {
        // "app" sits inside "WhatsApp", "Müller App" has it at the start of a word.
        val result = TextSearch.filter(apps, "app") { it.label }
        assertEquals(listOf("Müller App", "WhatsApp"), result.map { it.label })
    }

    @Test
    fun `the search finds a hit in the middle of a word too`() {
        // exactly what a pure prefix search cannot do - and why this one exists.
        assertEquals(listOf("Photo Editor"), TextSearch.filter(apps, "dito") { it.label }.map { it.label })
    }

    @Test
    fun `upper and lower case make no difference`() {
        assertEquals(
            TextSearch.filter(apps, "BRAVE") { it.label }.map { it.label },
            TextSearch.filter(apps, "brave") { it.label }.map { it.label },
        )
    }

    @Test
    fun `umlauts are found without typing umlauts`() {
        assertEquals(listOf("Müller App"), TextSearch.filter(apps, "muller") { it.label }.map { it.label })
        assertEquals(listOf("Öffi"), TextSearch.filter(apps, "offi") { it.label }.map { it.label })
    }

    @Test
    fun `eszett counts as double s`() {
        val list = listOf(app("Straße"))
        assertEquals(1, TextSearch.filter(list, "strasse") { it.label }.size)
    }

    @Test
    fun `several word parts must all match`() {
        assertEquals(listOf("Google Maps"), TextSearch.filter(apps, "goog map") { it.label }.map { it.label })
        assertTrue(TextSearch.filter(apps, "goog zzz") { it.label }.isEmpty())
    }

    @Test
    fun `without a hit the list stays empty`() {
        assertTrue(TextSearch.filter(apps, "xyzzy") { it.label }.isEmpty())
    }

    @Test
    fun `the rank tells start, word start and middle apart`() {
        assertEquals(0, TextSearch.rank("Google Maps", "goo"))
        assertEquals(1, TextSearch.rank("Google Maps", "map"))
        assertEquals(2, TextSearch.rank("WhatsApp", "atsa"))
        assertNull(TextSearch.rank("WhatsApp", "zzz"))
    }

    @Test
    fun `a separator counts as a word boundary`() {
        assertEquals(1, TextSearch.rank("Firefox-Klar", "klar"))
        assertEquals(1, TextSearch.rank("org.my_service", "service"))
    }

    @Test
    fun `at equal rank it sorts alphabetically`() {
        val list = listOf(app("Zebra Tool"), app("Alpha Tool"), app("Middle Tool"))
        assertEquals(
            listOf("Alpha Tool", "Middle Tool", "Zebra Tool"),
            TextSearch.filter(list, "tool") { it.label }.map { it.label },
        )
    }

    @Test
    fun `the order stays stable between two key presses`() {
        // otherwise the list jumps away under the finger - on three inches especially annoying.
        val once = TextSearch.filter(apps, "a") { it.label }.map { it.label }
        val twice = TextSearch.filter(apps.shuffled(), "a") { it.label }.map { it.label }
        assertEquals(once, twice)
    }
}

/**
 * searching until exactly one is left.
 *
 * on three inches the keyboard covers the hit list entirely - twenty pixels are left between
 * search field and keyboard. the way out is not more room, which does not exist, but: the
 * number of hits stands in the field, and at exactly one the magnifier key starts it.
 */
class SingleMatchTest {

    private val apps = listOf(
        "AnkiDroid", "Assistant", "BigLau", "Bolt", "Brave", "Calculator", "Calendar",
        "Camera", "Chrome", "Clock", "Gmail", "Google Maps", "MeteoSwiss",
        "Microsoft SwiftKey Keyboard", "Settings",
    )

    private fun hits(query: String) = TextSearch.filter(apps, query) { it }

    @Test
    fun `four letters are enough for a single hit`() {
        assertEquals(listOf("Calculator"), hits("calc"))
    }

    @Test
    fun `three letters still leave two here`() {
        // "cal" hits Calculator and Calendar - the number in the field says so, and the
        // magnifier key then does nothing instead of starting one of them at random.
        assertEquals(2, hits("cal").size)
        assertNull(hits("cal").singleOrNull())
    }

    @Test
    fun `two word parts lead to the target faster`() {
        assertEquals(listOf("Google Maps"), hits("goog map"))
    }

    @Test
    fun `a word in the middle counts too`() {
        assertEquals(listOf("Microsoft SwiftKey Keyboard"), hits("swift"))
    }

    @Test
    fun `without a hit nothing is left`() {
        assertEquals(emptyList<String>(), hits("zzz"))
        assertNull(hits("zzz").singleOrNull())
    }

    @Test
    fun `the empty query shows everything`() {
        assertEquals(apps.size, hits("").size)
    }
}
