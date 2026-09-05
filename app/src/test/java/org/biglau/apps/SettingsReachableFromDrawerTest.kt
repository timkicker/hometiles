package org.biglau.apps

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * from the app list one always gets into the settings.
 *
 * looked at on the device because the diagnostics page could not be found: eight filled
 * tiles, none of them the settings, swiping between screens off, no free slot to long press.
 * the app list offered the row only **if** apps were hidden.
 *
 * exactly one way was left: long press an existing tile and reassign it - give up an app, and
 * think of that first. `PLAN.md` 5 puts it as a rule: no phase may leave the device in a state
 * one cannot get out of without a computer. strictly one could - through a door nobody
 * recognises as one.
 */
class SettingsReachableFromDrawerTest {

    private val file = Quelltext.file("org/biglau/apps/AppDrawerActivity.kt")
    private val list = file.readText()
    private val lines = file.readLines()

    @Test
    fun `the app list offers the way into the settings`() {
        assertTrue(
            "the app list no longer calls SettingsLink.toRoot. whoever gives away their " +
                "settings tile then has no way there at all.",
            "SettingsLink.toRoot" in list,
        )
    }

    /**
     * the condition before it may speak **only** of the search.
     *
     * the first version demanded the literal `query.isEmpty()` and so nailed down the
     * implementation instead of the intention: when the row was improved to also stand there
     * when the **search matches it**, the rule fell over although the thing had got better.
     */
    @Test
    fun `the way hangs on no condition but the search`() {
        // the **row**, not any call of `SettingsLink.toRoot` - there is a second one (the
        // search key) and `indexOf` found that one first, so the rule read a condition that
        // did not belong to the row.
        val atLine = lines.indexOfFirst { "label = settingsLabel" in it }
        assertTrue("the row into the settings no longer exists", atLine > 0)
        val condition = lines.subList(maxOf(0, atLine - 8), atLine)
            .reversed()
            .firstNotNullOfOrNull { Regex("""if \((.+?)\) \{""").find(it)?.groupValues?.get(1) }
        assertTrue("the row hangs on no condition any more", condition != null)

        // a name is not a condition but points at one: the second version read the wording and
        // fell over when the condition got a name, the thing having stayed the same.
        val wording = if (Regex("""^\w+$""").matches(condition!!)) {
            lines.firstOrNull { it.trim().startsWith("val $condition ") }
                ?: condition
        } else {
            condition
        }
        assertTrue(
            "the way into the settings hangs on a condition that is not about the search: " +
                "\"if ($condition)\" -> $wording. it is meant to be there always; only a " +
                "search that does not match it may leave it out.",
            "query" in wording,
        )
    }
}
