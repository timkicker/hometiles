package org.biglau.apps

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the row about hidden apps stands there while somebody is searching too.
 *
 * it hung on `query.isEmpty()` - so it was gone exactly when it is needed. whoever types an
 * app's name and hid that app months ago read that no app matches and had no clue left.
 *
 * the settings row one line above had the same fault and was changed for it; the reasoning
 * stands there in the source. this rule holds that the two do not drift apart again.
 */
class HiddenRowTest {

    private val list = Quelltext.withoutComments("org/biglau/apps/AppDrawerActivity.kt")

    @Test
    fun `while searching, matching hidden apps are named`() {
        assertTrue(
            "the app list does not ask for hidden hits - then it says no app matches while " +
                "a hidden one does.",
            "AppDrawer.hiddenMatches(" in list,
        )
        assertTrue(
            "the matching hidden apps are not shown.",
            "R.plurals.apps_hidden_match" in list,
        )
        // the first version of this rule only checked that the words stood somewhere in the
        // file. the counter-check - setting the search case to `null` - passed: the row was
        // gone again, the rule green. now the decision itself is read.
        val decision = Quelltext.cut(list, "val hiddenRow", "}")
        assertTrue(
            "the row does not hang on the search case - it would be gone while searching, " +
                "so exactly when it is needed: $decision",
            "query.isNotEmpty()" in decision && "hiddenHits" in decision,
        )
    }

    @Test
    fun `no contradiction between the two sentences`() {
        val empty = Quelltext.cut(list, "R.string.search_no_match", atMost = 0)
        // the sentence stands inside an `if` beginning a few lines above.
        val condition = Quelltext.cut(list, "", "R.string.search_no_match").takeLast(300)
        assertTrue(
            "\"no app matches\" stands there even when a hidden app does - then the two " +
                "rows contradict each other side by side: $condition$empty",
            "hiddenHits.isEmpty()" in condition,
        )
    }
}
