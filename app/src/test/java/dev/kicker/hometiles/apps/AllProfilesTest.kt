package dev.kicker.hometiles.apps

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the app list asks every profile, not only the own one (issue #6).
 *
 * every call in `AppRepository` asked for `Process.myUserHandle()`, and a work profile's
 * apps were simply not there. nothing was wrong that a test of the list would have seen: the
 * main profile's apps all came. so the rule reads the source.
 */
class AllProfilesTest {

    /** where asking for the own profile alone is right, and why. */
    private val allowed = mapOf(
        "AppRepository.kt" to "names the main profile, which every other is told apart from",
        "BigNotificationListener.kt" to "tells a notice from the main profile apart from a work one",
        "ShortcutRepository.kt" to "shortcuts are offered for main profile apps only, see AppList",
    )

    @Test
    fun `only named places ask for the own profile`() {
        val hits = Quelltext.files()
            .filter { "Process.myUserHandle()" in it.readText() }
            .map { it.name }
            .toSortedSet()
        assertEquals(
            "these files ask for the own profile only. a work profile's apps go missing there.",
            allowed.keys.toSortedSet(),
            hits,
        )
    }

    @Test
    fun `the list is read for every profile`() {
        val repository = Quelltext.withoutComments("dev/kicker/hometiles/apps/AppRepository.kt")
        assertTrue("loadApps no longer walks the profiles", "profiles().flatMap" in repository)
        assertTrue("the profiles no longer come from the system", "users.userProfiles" in repository)
    }

    /** the system's own launch path knows the main profile only: a work tile would open the private app. */
    @Test
    fun `a work app never falls back to the main profile`() {
        val repository = Quelltext.withoutComments("dev/kicker/hometiles/apps/AppRepository.kt")
        val guard = repository.indexOf("if (user != main)")
        val fallback = repository.indexOf("getLaunchIntentForPackage")
        assertTrue("the guard before the system path is gone", guard in 0 until fallback)
    }
}
