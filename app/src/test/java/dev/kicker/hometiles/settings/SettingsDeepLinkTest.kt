package dev.kicker.hometiles.settings

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the jump to a sub page - and what kept it from arriving for a long time.
 *
 * measured: started cold it jumped, warm it did not. android delivers a new intent to an
 * already running activity only through `onNewIntent`, and `onNewIntent` comes only with a
 * launch mode that reuses the existing instance. without `singleTop` the system discarded
 * the call and the earlier page stayed - a switch that works once and never again.
 */
class SettingsDeepLinkTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `a known name gives the page`() {
        assertEquals(Page.CALL_TYPES, SettingsDeepLink.target("CALL_TYPES"))
        assertEquals(Page.CONTACTS, SettingsDeepLink.target("CONTACTS"))
    }

    /** a backup from a later version must not shoot down the settings. */
    @Test
    fun `an unknown or missing name gives nothing`() {
        assertNull(SettingsDeepLink.target("GIBTSNICHT"))
        assertNull(SettingsDeepLink.target(null))
        assertNull(SettingsDeepLink.target(""))
    }

    @Test
    fun `without a target the main page opens`() {
        assertEquals(Page.MAIN, SettingsDeepLink.start(locked = false, target = null))
    }

    @Test
    fun `with a target the sub page opens`() {
        assertEquals(Page.MESSAGES, SettingsDeepLink.start(locked = false, target = Page.MESSAGES))
    }

    /** otherwise the pin would be bypassed by a call from outside. */
    @Test
    fun `the lock comes before the target`() {
        assertEquals(Page.GATE, SettingsDeepLink.start(locked = true, target = Page.MESSAGES))
        assertNull(SettingsDeepLink.jump(Page.GATE, Page.MESSAGES))
    }

    @Test
    fun `a later request jumps`() {
        assertEquals(Page.CALL_TYPES, SettingsDeepLink.jump(Page.MESSAGES, Page.CALL_TYPES))
    }

    @Test
    fun `without a request and on its own page everything stays`() {
        assertNull(SettingsDeepLink.jump(Page.MESSAGES, null))
        assertNull(SettingsDeepLink.jump(Page.MESSAGES, Page.MESSAGES))
    }

    /** without a reusing launch mode the second request never arrives. */
    @Test
    fun `the settings accept a second call`() {
        val block = Quelltext.cut(manifest, ".settings.SettingsActivity", "/>")
        assertTrue(
            "SettingsActivity accepts a deep link and therefore needs a launch mode where " +
                "onNewIntent arrives: $block",
            "singleTop" in block || "singleTask" in block,
        )
    }

    /**
     * `SettingsLink` is read, not `SettingsActivity`: the identifiers stand there since the
     * jump runs through an intent, and a test looking for strings in `SettingsActivity`
     * would find **nothing** and be green without checking anything.
     */
    @Test
    fun `every offered identifier belongs to a page`() {
        val names = Page.entries.map { it.name }
        val hits = Regex("""const val PAGE_[A-Z_]+ = "([A-Z_]+)"""")
            .findAll(Quelltext.file("dev/kicker/hometiles/ui/SettingsLink.kt").readText())
            .toList()
        assertTrue("not a single identifier found - does the test read the right file?", hits.isNotEmpty())
        hits.forEach {
            val value = it.groupValues[1]
            assertTrue("PAGE identifier \"$value\" belongs to no page", value in names)
        }
    }
}
