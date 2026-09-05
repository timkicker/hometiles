package org.biglau.phone

import android.telecom.CallAudioState
import org.biglau.data.AudioRoute

/**
 * translates between the setting and what telecom knows.
 *
 * this mapping stood in the source twice and the second copy was only half written: it had
 * "speaker on" but not "speaker off". such tables belong in one place.
 */
object AudioRoutes {

    fun toTelecom(route: AudioRoute): Int = when (route) {
        AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
        AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
        AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
    }

    /** anything unknown counts as the earpiece. */
    fun fromTelecom(route: Int?): AudioRoute = when (route) {
        CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
        CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
        else -> AudioRoute.EARPIECE
    }

    /**
     * asks the **list of possible routes**, not the current one: bluetooth appears there
     * only once a device is connected, and only then is the choice worth offering.
     */
    fun bluetoothAvailable(supportedMask: Int?): Boolean =
        supportedMask != null && supportedMask and CallAudioState.ROUTE_BLUETOOTH != 0
}
