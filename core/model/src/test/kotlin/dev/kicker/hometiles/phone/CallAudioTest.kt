package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.AudioRoute
import dev.kicker.hometiles.data.PhoneConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * where the sound goes when a call connects (PLAN.md 4.6).
 *
 * two promises meet here, and the order between them is the actual decision: speaker on
 * outgoing calls is the more specific case and beats the general default output.
 */
class CallAudioTest {

    @Test
    fun `without a setting nothing is changed`() {
        // the earpiece is the system's default anyway - then the most honest intervention is
        // none at all.
        assertNull(CallAudio.routeOnConnect(PhoneConfig(), outgoing = false))
        assertNull(CallAudio.routeOnConnect(PhoneConfig(), outgoing = true))
    }

    @Test
    fun `the default output holds for both directions`() {
        val config = PhoneConfig(audioRoute = AudioRoute.BLUETOOTH)
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = false))
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = true))
    }

    @Test
    fun `speaker on dialling holds only for one's own calls`() {
        val config = PhoneConfig(speakerOnOutgoing = true)
        assertEquals(AudioRoute.SPEAKER, CallAudio.routeOnConnect(config, outgoing = true))
        assertNull(CallAudio.routeOnConnect(config, outgoing = false))
    }

    @Test
    fun `on dialling the speaker beats the default output`() {
        // whoever dials themselves often still holds the phone in their hand and looks at it.
        val config = PhoneConfig(audioRoute = AudioRoute.BLUETOOTH, speakerOnOutgoing = true)
        assertEquals(AudioRoute.SPEAKER, CallAudio.routeOnConnect(config, outgoing = true))
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = false))
    }

    @Test
    fun `the earpiece chosen expressly stays a non-intervention`() {
        val config = PhoneConfig(audioRoute = AudioRoute.EARPIECE, speakerOnOutgoing = false)
        assertNull(CallAudio.routeOnConnect(config, outgoing = true))
    }
}
