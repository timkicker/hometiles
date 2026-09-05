package org.biglau.safety

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * out of the emergency mode one reaches the phone.
 *
 * the emergency catch appears when BigLau twice in a row does not get as far as drawing.
 * until 03.09.2026 it offered four ways - try again, settings, pick another home screen,
 * reset the tiles. all four repair the launcher; none helped the person who wants to make a
 * **call** at that moment, though the README promised "a simple screen with phone, contacts
 * and settings".
 *
 * deliberately the **system's** apps: the app's own dial pad is exactly what one should not
 * rely on right after the app has crashed twice.
 */
class EmergencyReachTest {

    private val source = Quelltext.file("org/biglau/safety/EmergencyScreen.kt").readText()

    @Test
    fun `the emergency mode leads to the phone and to the contacts`() {
        assertTrue("no way to the dial pad", "Intents.openDialer(" in source)
        assertTrue("no way to the contacts", "Intents.openContacts(" in source)
    }

    /** and **before** the repair buttons: whoever wants to call should not have to read past
     * "pick another home screen" first. */
    @Test
    fun `the phone stands before the repair`() {
        val phone = source.indexOf("R.string.emergency_phone")
        val again = source.indexOf("R.string.emergency_retry")
        assertTrue("emergency_phone is missing", phone >= 0)
        assertTrue("emergency_retry is missing", again >= 0)
        assertTrue("the phone button stands behind the repair buttons", phone < again)
    }

    /** the emergency mode does **not** load the configuration - it could be the problem
     * itself. hence its fixed colours. */
    @Test
    fun `the emergency mode does not read the configuration`() {
        assertTrue("ConfigStore in the emergency mode", "ConfigStore" !in source)
    }
}
