package dev.kicker.hometiles.phone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallProximityTest {

    @Test
    fun `a call at the ear turns the screen off`() {
        // issue #4: the big buttons get pressed by the cheek.
        for (status in listOf(CallStatus.DIALING, CallStatus.CONNECTING, CallStatus.ACTIVE, CallStatus.HOLDING)) {
            assertTrue("$status", CallProximity.screenOff(status, earpiece = true))
        }
    }

    @Test
    fun `a ringing call keeps the screen`() {
        // who calls and the answer button have to be seen, even in a hand that covers the sensor.
        assertFalse(CallProximity.screenOff(CallStatus.RINGING, earpiece = true))
    }

    @Test
    fun `away from the earpiece the screen stays`() {
        // speaker, bluetooth or a headset: the phone is not at the ear, and the screen is how
        // one gets back to the buttons.
        assertFalse(CallProximity.screenOff(CallStatus.ACTIVE, earpiece = false))
    }

    @Test
    fun `an ended call lets go`() {
        for (status in listOf(CallStatus.DISCONNECTING, CallStatus.DISCONNECTED, CallStatus.OTHER)) {
            assertFalse("$status", CallProximity.screenOff(status, earpiece = true))
        }
    }

    @Test
    fun `no call no lock`() {
        assertFalse(CallProximity.screenOff(null, earpiece = true))
    }
}
