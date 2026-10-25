package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.data.PhoneConfig
import dev.kicker.hometiles.data.SpeedDialTarget
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a set speed dial can be removed again.
 *
 * the clear row stood on the dialler's assign page, which only a long press on an *empty*
 * key opens; on a set key the long press dials. so a speed dial, once set, stayed for good
 * (issue #2). the way out is the phone page of the settings.
 */
class SpeedDialClearTest {

    private val settings = Quelltext.file("dev/kicker/hometiles/settings/SettingsActivity.kt").readText()

    @Test
    fun `the settings list every set key and clear it`() {
        assertTrue("the settings do not go through the assignable keys", "SpeedDial.ASSIGNABLE" in settings)
        assertTrue("nothing in the settings clears a speed dial", "SpeedDial.clear(phone, key)" in settings)
    }

    @Test
    fun `clearing leaves the key empty`() {
        val set = SpeedDial.assign(PhoneConfig(), '2', SpeedDialTarget("Anna", "+447700900123"))
        assertNull(SpeedDial.targetFor(SpeedDial.clear(set, '2'), '2'))
    }
}
