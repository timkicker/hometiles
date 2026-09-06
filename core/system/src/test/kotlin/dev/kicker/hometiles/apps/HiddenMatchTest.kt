package dev.kicker.hometiles.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a hidden app is not gone, and the search says so.
 *
 * "no app matches" is true of the visible list and untrue of the phone. whoever hid an app
 * months ago and looks for it otherwise faces a sentence sending them the wrong way - and the
 * row about hidden apps was missing exactly then, because it hung on `query.isEmpty()`.
 */
class HiddenMatchTest {

    private fun app(pkg: String, name: String) =
        LaunchableApp(packageName = pkg, activityName = "$pkg.Main", label = name)

    private val all = listOf(
        app("com.brave.browser", "Brave"),
        app("com.example.bank", "Bank"),
        app("com.example.maps", "Maps"),
    )

    @Test
    fun `a hidden app counts as a match, but not as a visible one`() {
        val hidden = setOf(AppDrawer.keyOf(all[0]))
        assertTrue(
            "a hidden app must not turn up in the list",
            AppDrawer.search(all, hidden, "brave").isEmpty(),
        )
        assertEquals(
            "...but the search must know it exists",
            listOf("Brave"),
            AppDrawer.hiddenMatches(all, hidden, "brave").map { it.label },
        )
    }

    @Test
    fun `without a query there are no matches to report`() {
        val hidden = setOf(AppDrawer.keyOf(all[0]))
        assertEquals(emptyList<String>(), AppDrawer.hiddenMatches(all, hidden, "").map { it.label })
        assertEquals(emptyList<String>(), AppDrawer.hiddenMatches(all, hidden, "   ").map { it.label })
    }

    @Test
    fun `what is visible does not count here`() {
        assertEquals(
            "a visible app must not count as a hidden match - the row would then stand " +
                "beside the app one is looking at.",
            emptyList<String>(),
            AppDrawer.hiddenMatches(all, emptySet(), "bank").map { it.label },
        )
    }
}
