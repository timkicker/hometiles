package org.biglau.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.biglau.ui.EMERGENCY_HOLD_MILLIS
import org.junit.Test

class PinTest {

    @Test
    fun `four to eight digits are valid`() {
        assertTrue(Pin.isValid("1234"))
        assertTrue(Pin.isValid("12345678"))
        assertTrue(!Pin.isValid("123"))
        assertTrue(!Pin.isValid("123456789"))
        assertTrue(!Pin.isValid(""))
    }

    @Test
    fun `letters are no pin`() {
        assertTrue(!Pin.isValid("12a4"))
        assertTrue(!Pin.isValid("abcd"))
        assertTrue(!Pin.isValid("12 34"))
    }

    @Test
    fun `an invalid pin cannot be stored`() {
        assertNull(Pin.hash("123"))
        assertNull(Pin.hash("abcd"))
    }

    @Test
    fun `the right pin is recognised`() {
        val stored = Pin.hash("2468")!!
        assertTrue(Pin.verify("2468", stored))
    }

    @Test
    fun `a wrong pin is refused`() {
        val stored = Pin.hash("2468")!!
        assertTrue(!Pin.verify("2469", stored))
        assertTrue(!Pin.verify("246", stored))
        assertTrue(!Pin.verify("", stored))
    }

    @Test
    fun `without a pin set everything is open`() {
        assertTrue(Pin.verify("", null))
        assertTrue(Pin.verify("9999", null))
    }

    @Test
    fun `the same pin gives two different values`() {
        // otherwise two backup files would show that the same pin holds.
        val first = Pin.hash("1234")!!
        val second = Pin.hash("1234")!!
        assertTrue(first != second)
        assertTrue(Pin.verify("1234", first))
        assertTrue(Pin.verify("1234", second))
    }

    @Test
    fun `the pin does not stand in clear text in the stored value`() {
        val stored = Pin.hash("13579")!!
        assertTrue(!stored.contains("13579"))
    }

    @Test
    fun `the stored value has three parts separated by colons`() {
        val parts = Pin.hash("1234")!!.split(":")
        assertEquals(3, parts.size)
        assertEquals(20_000, parts[0].toInt())
    }

    @Test
    fun `a broken stored value locks instead of opening`() {
        // locking when in doubt is right: a broken file must not release the settings by
        // accident. the emergency exit stays the way back.
        listOf("", "kaputt", "1:2", "a:b:c", "20000:!!!:???").forEach { broken ->
            assertTrue("'$broken' should have locked", !Pin.verify("1234", broken))
        }
    }

    @Test
    fun `a different salt gives a different hash`() {
        val a = Pin.hash("1234", ByteArray(16) { 1 })!!
        val b = Pin.hash("1234", ByteArray(16) { 2 })!!
        assertTrue(a != b)
    }

    @Test
    fun `the same salt gives the same hash`() {
        val salt = ByteArray(16) { 7 }
        assertEquals(Pin.hash("1234", salt), Pin.hash("1234", salt))
    }
}

/**
 * how long the emergency exit is held.
 *
 * the number stands in the lock's explainer, and that text has to be right: whoever holds for
 * 30 seconds because it says so and lets go after 25 because nothing happens believes
 * themselves locked out. the number in the text and the number in the code must not drift.
 */
class EmergencyHoldTest {

    @Test
    fun `the emergency exit takes thirty seconds`() {
        assertEquals(30_000L, EMERGENCY_HOLD_MILLIS)
    }

    /**
     * and `PLAN.md` names the same number - in **every** place it occurs. the 30 stood here
     * and three times in the plan with no connection between them; changing the plan to 20
     * would have got no word from any test while the lock stayed at 30.
     *
     * the pattern is german because the plan is: it reads PLAN.md, not source.
     */
    @Test
    fun `the plan names the same duration`() {
        val plan = java.io.File("../PLAN.md").readText()
        val numbers = Regex("""(\d+)[ -]Sekunden?-?Notausstieg|Notausstieg[^.\n]*?(\d+) ?s(?:ekunden)?\b""")
            .findAll(plan)
            .mapNotNull { hit ->
                hit.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.toLong()
            }
            .toList()
        org.junit.Assert.assertTrue(
            "the plan names no duration for the emergency exit any more - then it cannot " +
                "drift either, but that was not the intention.",
            numbers.isNotEmpty(),
        )
        numbers.forEach {
            assertEquals(
                "PLAN.md says $it seconds, the lock holds ${EMERGENCY_HOLD_MILLIS / 1000}",
                EMERGENCY_HOLD_MILLIS / 1000,
                it,
            )
        }
    }

    @Test
    fun `the duration comes out whole in seconds`() {
        // the countdown counts down in whole seconds; an odd value would leave it standing at
        // 1 instead of firing at 0.
        assertEquals(0L, EMERGENCY_HOLD_MILLIS % 1000)
    }
}

/**
 * the pin protection for the tile editor.
 *
 * `PLAN.md` 4.5 promises it; the field stood in the model from the first day and was read
 * nowhere. the point is not secrecy but that the tile layout is not taken apart by accident -
 * a long press happens faster than one thinks.
 */
class EditorProtectionTest {

    private val set = Pin.hash("1234")

    @Test
    fun `without a pin nothing is protected`() {
        assertFalse(Pin.protectsEditor(null, enabled = true))
        assertFalse(Pin.protectsEditor(null, enabled = false))
    }

    @Test
    fun `with a pin and switched on it asks`() {
        assertTrue(Pin.protectsEditor(set, enabled = true))
    }

    @Test
    fun `with a pin and switched off it does not`() {
        // whoever locks the settings does not necessarily want the tiles locked too.
        assertFalse(Pin.protectsEditor(set, enabled = false))
    }
}
