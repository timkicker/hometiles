package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.data.PhoneConfig
import dev.kicker.hometiles.phone.SpeedDial
import dev.kicker.hometiles.data.SpeedDialTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the hint says what holding does **now**.
 *
 * a long press on an empty digit leads to assigning it; on an assigned one it dials **at
 * once**, without a confirmation. above the keypad the same sentence stood in both cases -
 * the same in the harmless state as in the dangerous one, and it named the function rather
 * than the gesture's consequence.
 *
 * that does not settle the open question whether the speed dial should ask before dialling
 * (that belongs to the user, see `STATUS.md`). it only makes sure they decide it knowing
 * that the call log asks before dialling and the speed dial does not.
 */
class SpeedDialHintStateTest {

    private val dialer = Quelltext.withoutComments("dev/kicker/hometiles/phone/DialerActivity.kt")

    @Test
    fun `empty and assigned get different sentences`() {
        assertTrue(
            "the hint above the keypad is fixed again - then the harmless state carries " +
                "the same sentence as the dangerous one.",
            "R.string.dialer_speeddial_hint_assign" in dialer &&
                "R.string.dialer_speeddial_hint_call" in dialer,
        )
        assertTrue(
            "the hint does not ask whether any key is assigned at all.",
            "anySpeedDial" in dialer,
        )
    }

    @Test
    fun `anyAssigned counts only the assignable keys`() {
        val empty = PhoneConfig()
        assertFalse("without a speed dial nothing may be assigned", SpeedDial.anyAssigned(empty))

        val assigned = SpeedDial.assign(empty, '3', SpeedDialTarget(name = "Anna", number = "+430000"))
        assertTrue("after assigning it has to show", SpeedDial.anyAssigned(assigned))

        val cleared = SpeedDial.clear(assigned, '3')
        assertFalse("after clearing no longer", SpeedDial.anyAssigned(cleared))
    }

    /** and the old fixed sentence is really gone, not merely left lying unused. */
    @Test
    fun `the fixed sentence stands nowhere any more`() {
        val left = Quelltext.allTexts()
            .filter { "dialer_speeddial_hint\"" in it.readText() }
            .map { it.parentFile.name + "/" + it.name }
        assertEquals("the old hint is still lying around", emptyList<String>(), left)
    }
}
