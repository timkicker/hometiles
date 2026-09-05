package org.biglau.phone

import android.telecom.CallAudioState
import org.biglau.data.AudioRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the mapping between setting and telecom stood twice in the source - and the second time
 * only by half: speaker on was there, speaker off was not.
 */
class AudioRoutesTest {

    @Test
    fun `every route has its counterpart`() {
        assertEquals(CallAudioState.ROUTE_EARPIECE, AudioRoutes.toTelecom(AudioRoute.EARPIECE))
        assertEquals(CallAudioState.ROUTE_SPEAKER, AudioRoutes.toTelecom(AudioRoute.SPEAKER))
        assertEquals(CallAudioState.ROUTE_BLUETOOTH, AudioRoutes.toTelecom(AudioRoute.BLUETOOTH))
    }

    @Test
    fun `the way back matches the way there`() {
        AudioRoute.entries.forEach { route ->
            assertEquals(route, AudioRoutes.fromTelecom(AudioRoutes.toTelecom(route)))
        }
    }

    @Test
    fun `the unknown counts as the earpiece`() {
        // a route this app does not know (wired headphones, say) must not set the display to
        // speaker - that would be an answer that is not true.
        assertEquals(AudioRoute.EARPIECE, AudioRoutes.fromTelecom(null))
        assertEquals(AudioRoute.EARPIECE, AudioRoutes.fromTelecom(CallAudioState.ROUTE_WIRED_HEADSET))
    }

    @Test
    fun `bluetooth counts only when the device offers it`() {
        assertFalse(AudioRoutes.bluetoothAvailable(null))
        assertFalse(
            AudioRoutes.bluetoothAvailable(
                CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
            ),
        )
        assertTrue(
            AudioRoutes.bluetoothAvailable(
                CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_BLUETOOTH,
            ),
        )
    }
}
