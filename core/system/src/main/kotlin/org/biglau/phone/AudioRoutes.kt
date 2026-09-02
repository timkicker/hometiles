package org.biglau.phone

import android.telecom.CallAudioState
import org.biglau.data.AudioRoute

/**
 * Uebersetzt zwischen der Einstellung und dem, was Telecom kennt.
 *
 * Die Zuordnung stand zweimal im Quelltext - einmal beim Verbinden, einmal beim Umschalten
 * von Hand - und wurde beim zweiten Mal nur zur Haelfte hingeschrieben: „Lautsprecher an"
 * gab es, „Lautsprecher aus" nicht. Solche Tabellen gehoeren an eine Stelle.
 */
object AudioRoutes {

    fun toTelecom(route: AudioRoute): Int = when (route) {
        AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
        AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
        AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
    }

    /** Was Telecom gerade meldet. Unbekanntes zaehlt als Hoermuschel. */
    fun fromTelecom(route: Int?): AudioRoute = when (route) {
        CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
        CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
        else -> AudioRoute.EARPIECE
    }

    /**
     * Steckt Bluetooth in dem, was das Geraet anbietet?
     *
     * Gefragt wird die **Liste der moeglichen Wege**, nicht der derzeitige: erst wenn ein
     * Geraet verbunden ist, taucht Bluetooth darin auf - und nur dann lohnt die Auswahl.
     */
    fun bluetoothAvailable(supportedMask: Int?): Boolean =
        supportedMask != null && supportedMask and CallAudioState.ROUTE_BLUETOOTH != 0
}
