package org.biglau.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCountsTest {

    private fun row(
        pkg: String,
        clearable: Boolean = true,
        ongoing: Boolean = false,
        groupSummary: Boolean = false,
        category: String? = null,
        mediaStyle: Boolean = false,
    ) = NotificationRow(pkg, clearable, ongoing, groupSummary, 0, category, mediaStyle)

    /**
     * paused playback: the case that blinked on 04.09.2026. while the player runs the notice
     * is `ongoing` and already falls through the old rule. paused it is not, can be swiped
     * away and so looked like a waiting message - though it is the same notice and reports
     * nothing new.
     */
    @Test
    fun `paused playback does not count`() {
        val paused = row("musik", clearable = true, ongoing = false, mediaStyle = true)
        assertTrue("the template alone has to be enough", !NotificationCounts.counts(paused))
        assertEquals(
            emptyMap<String, Int>(),
            NotificationCounts.summarise(listOf(paused)),
        )
    }

    @Test
    fun `notices about something running do not count`() {
        // what the app itself gives as its category. everything here is a notice about
        // something running or holding, not a message anyone would answer.
        listOf("transport", "service", "progress", "navigation", "call", "alarm", "sys").forEach {
            assertTrue(
                "category $it must not blink",
                !NotificationCounts.counts(row("app", category = it)),
            )
        }
    }

    @Test
    fun `a message still counts`() {
        // the counter-check: without it the list could grow too wide and filter everything
        // away.
        listOf(null, "msg", "email", "social", "event", "reminder").forEach {
            assertTrue(
                "category $it is a message and has to count",
                NotificationCounts.counts(row("app", category = it)),
            )
        }
    }

    @Test
    fun `ordinary messages are counted per package`() {
        val counts = NotificationCounts.summarise(
            listOf(row("chat"), row("chat"), row("mail")),
        )
        assertEquals(mapOf("chat" to 2, "mail" to 1), counts)
    }

    @Test
    fun `running notices do not count`() {
        // music playback and usb debugging are on permanently. if they counted, the tile
        // would blink for ever - and the user learns to ignore it.
        val counts = NotificationCounts.summarise(
            listOf(row("player", ongoing = true), row("player")),
        )
        assertEquals(mapOf("player" to 1), counts)
    }

    @Test
    fun `notices that cannot be swiped away do not count`() {
        assertTrue(NotificationCounts.summarise(listOf(row("system", clearable = false))).isEmpty())
    }

    @Test
    fun `group summaries do not count twice`() {
        // many apps report three messages plus a summary. without this rule the tile would
        // show a four.
        val counts = NotificationCounts.summarise(
            listOf(row("chat"), row("chat"), row("chat"), row("chat", groupSummary = true)),
        )
        assertEquals(mapOf("chat" to 3), counts)
    }

    @Test
    fun `an empty list gives no counters`() {
        assertTrue(NotificationCounts.summarise(emptyList()).isEmpty())
    }

    @Test
    fun `a package without a countable notice does not appear at all`() {
        // do not enter it with a zero, otherwise the tile blinks briefly on every change.
        val counts = NotificationCounts.summarise(listOf(row("player", ongoing = true)))
        assertTrue(!counts.containsKey("player"))
    }

    @Test
    fun `the badge stacks from ten on`() {
        assertNull(NotificationCounts.badgeText(0))
        assertNull(NotificationCounts.badgeText(-1))
        assertEquals("1", NotificationCounts.badgeText(1))
        assertEquals("9", NotificationCounts.badgeText(9))
        assertEquals("9+", NotificationCounts.badgeText(10))
        assertEquals("9+", NotificationCounts.badgeText(250))
    }

    @Test
    fun `the single check agrees with the summary`() {
        val rows = listOf(
            row("a"),
            row("b", ongoing = true),
            row("c", clearable = false),
            row("d", groupSummary = true),
        )
        val expected = rows.filter { NotificationCounts.counts(it) }.map { it.packageName }.toSet()
        assertEquals(expected, NotificationCounts.summarise(rows).keys)
    }
}
