package dev.kicker.hometiles.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** merging two calls into one (user mail 25.11.2026). */
class MergeCallsTest {

    private fun view(otherHeld: Boolean, canMerge: Boolean) = CallView(
        status = CallStatus.ACTIVE,
        number = "+447700900123",
        name = null,
        startedAtMillis = 0L,
        otherName = if (otherHeld) "Alex" else null,
        otherHeld = otherHeld,
        canMerge = canMerge,
    )

    @Test
    fun `two calls the network can merge offer merging`() {
        assertTrue(CallAction.MERGE in CallActions.availableFor(view(otherHeld = true, canMerge = true)))
    }

    @Test
    fun `no merge where the network does not offer it`() {
        // a button that does nothing is worse than none: telecom ignores the request then.
        val actions = CallActions.availableFor(view(otherHeld = true, canMerge = false))
        assertTrue(CallAction.MERGE !in actions)
        assertTrue(CallAction.KEYPAD in actions)
    }

    @Test
    fun `no merge with only one call`() {
        assertTrue(CallAction.MERGE !in CallActions.availableFor(view(otherHeld = false, canMerge = true)))
    }

    @Test
    fun `merging adds no row`() {
        // five rows fill the small phone with large text; a sixth pushed the last one off.
        val plain = CallActions.availableFor(view(otherHeld = false, canMerge = false))
        val two = CallActions.availableFor(view(otherHeld = true, canMerge = true))
        assertEquals(plain.size, two.size)
        assertEquals(CallAction.HANG_UP, two.first())
    }

    @Test
    fun `a conference is named by its members`() {
        assertEquals("Alex, +447700900124", ConferenceName.of(listOf("Alex", "+447700900124")))
        assertEquals("Alex", ConferenceName.of(listOf("", "Alex")))
        assertNull(ConferenceName.of(emptyList()))
    }
}
