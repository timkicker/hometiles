package org.biglau.security

import org.biglau.apps.AppLock
import org.biglau.data.AppsConfig
import org.biglau.data.LauncherConfig
import org.biglau.data.Security
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a pin that cannot be checked is no lock.
 *
 * the occasion: a hand-written configuration with the pin in clear text instead of as
 * rounds:salt:hash. after that the app lock refused every entry, the right one included, and
 * could not be opened again: [Pin.verify] bails out on a value that has not three parts. on
 * the home screen there is no emergency exit for that.
 */
class PinUsableTest {

    private val real = Pin.hash("4712")!!

    @Test
    fun `a hashed value can be checked`() {
        assertTrue(Pin.usable(real))
        assertTrue(Pin.verify("4712", real))
    }

    @Test
    fun `clear text and nonsense cannot be checked`() {
        assertFalse(Pin.usable(null))
        assertFalse(Pin.usable("4712"))
        assertFalse(Pin.usable(""))
        assertFalse(Pin.usable("a:b"))
        assertFalse(Pin.usable("no number:AAAA:AAAA"))
        assertFalse(Pin.usable("20000:no base64 !:AAAA"))
    }

    @Test
    fun `a value that cannot be checked protects nothing`() {
        assertTrue(Pin.protects(real, enabled = true))
        assertFalse(Pin.protects("4712", enabled = true))
        assertFalse(Pin.protects(real, enabled = false))
    }

    @Test
    fun `the app lock holds nobody with a value that cannot be checked`() {
        fun config(pin: String?) = LauncherConfig(
            security = Security(pin = pin),
            apps = AppsConfig(lockOthers = true, allowed = emptySet()),
        )
        assertTrue(AppLock.needsPin(config(real), "a/b", "a"))
        assertFalse(AppLock.needsPin(config("4712"), "a/b", "a"))
    }
}
