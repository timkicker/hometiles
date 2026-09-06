package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the emergency exit leads out of HomeTiles, not back into it.
 *
 * `openDialer` sent a bare `ACTION_DIAL` while the sentence above it said "the **system's**
 * dial pad". a bare `ACTION_DIAL` goes to the default phone app - and since 03.09.2026 that
 * is HomeTiles itself. the emergency exit would have led back into the app that just crashed
 * twice.
 *
 * on the device it did not show: the system took its own dialer that day. an observation
 * that comes out right by chance is no proof.
 */
class EmergencyExitTest {

    private val intents = Quelltext.withoutComments("dev/kicker/hometiles/actions/Intents.kt")
    private val emergency = Quelltext.withoutComments("dev/kicker/hometiles/safety/EmergencyScreen.kt")

    @Test
    fun `the emergency exit asks explicitly for a foreign app`() {
        val from = intents.indexOf("fun openDialer(")
        assertTrue("openDialer no longer exists", from > 0)
        val body = intents.substring(from, minOf(intents.length, from + 600))
        assertTrue(
            "openDialer sends a bare ACTION_DIAL. that goes to the default phone app - and " +
                "if HomeTiles is it, the emergency exit leads back into the crashed app.",
            "packageName != context.packageName" in body,
        )
    }

    @Test
    fun `the emergency mode offers phone and contacts`() {
        listOf("openDialer", "openContacts").forEach { way ->
            assertTrue(
                "the emergency mode no longer offers $way - then only repair stands there, " +
                    "and whoever needs help reaches nobody.",
                way in emergency,
            )
        }
    }

    /** a screen that appears after two crashes is the last place where anything may go out
     * unasked. */
    @Test
    fun `the emergency mode dials nothing`() {
        val dials = emergency.lines().withIndex()
            .filter { (_, line) -> "ACTION_CALL" in line || "Intents.call(" in line }
            .map { it.index + 1 }
        assertEquals(
            "the emergency mode dials. it should open, not call.",
            emptyList<Int>(),
            dials,
        )
    }
}
