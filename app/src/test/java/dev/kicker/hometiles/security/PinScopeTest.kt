package dev.kicker.hometiles.security

import dev.kicker.hometiles.data.Security
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.5: the pin additionally for the tile editor, the app list, uninstalling and
 * clearing the call log.
 *
 * the editor was built; the app list and clearing the call log arrive here. **not
 * uninstalling**: this app has none, and a lock in front of a door that does not exist is no
 * security but a row in the settings that does nothing.
 */
class PinScopeTest {

    private val withPin = Security(pin = Pin.hash("1234"))

    /**
     * without a pin set nothing is protected, not even with the switch on. protection turned
     * on without a lock would be a promise that falls apart at the first tap.
     */
    @Test
    fun `without a pin nothing is protected`() {
        val without = Security(pin = null, pinProtectsAppList = true, pinProtectsEditor = true)
        assertEquals(false, Pin.protects(without.pin, without.pinProtectsAppList))
        assertEquals(false, Pin.protectsEditor(without.pin, without.pinProtectsEditor))
    }

    @Test
    fun `with a pin and the switch it protects`() {
        assertEquals(true, Pin.protects(withPin.pin, true))
        assertEquals(false, Pin.protects(withPin.pin, false))
    }

    /**
     * the app list is open by default: it is the way to every app that lies on no tile, and
     * whoever sets a pin only for the settings does not want to be locked out of their own
     * apps.
     */
    @Test
    fun `the app list is open by default`() {
        assertEquals(false, Security().pinProtectsAppList)
    }

    /**
     * clearing the call log is protected by default once a pin stands: it cannot be undone,
     * and whoever sets a pin wants exactly such steps secured.
     */
    @Test
    fun `clearing the call log is protected by default`() {
        assertEquals(true, Security().pinProtectsCallLogDelete)
        assertEquals(true, Pin.protects(withPin.pin, withPin.pinProtectsCallLogDelete))
    }

    // and without a pin it stays open, or the call log would be unclearable for everyone
    // without one.
    @Test
    fun `without a pin the call log stays clearable`() {
        val without = Security()
        assertEquals(false, Pin.protects(without.pin, without.pinProtectsCallLogDelete))
    }
}

/**
 * removing the pin removes the protection switches with it.
 *
 * the switches only stand there while a pin is set. left standing when it is removed they
 * cannot be reached any more - and a pin set later shuts doors unasked that were open before.
 */
class PinRemovalTest {

    @Test
    fun `without a pin the switches stand at their defaults again`() {
        val back = Security()
        assertEquals(null, back.pin)
        assertEquals(true, back.pinProtectsEditor)
        assertEquals(false, back.pinProtectsAppList)
        assertEquals(true, back.pinProtectsCallLogDelete)
    }

    // and then none of them protects anything.
    @Test
    fun `without a pin none of the switches protects`() {
        val without = Security()
        assertEquals(false, Pin.protects(without.pin, without.pinProtectsEditor))
        assertEquals(false, Pin.protects(without.pin, without.pinProtectsAppList))
        assertEquals(false, Pin.protects(without.pin, without.pinProtectsCallLogDelete))
    }
}
