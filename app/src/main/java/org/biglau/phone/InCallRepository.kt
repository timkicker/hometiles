package org.biglau.phone

import android.telecom.Call
import org.biglau.data.AudioRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Der laufende Anruf, zwischen Dienst und Oberflaeche.
 *
 * Bewusst ein einzelner Anruf: Zweitanruf und Konferenz kommen spaeter und muessen dann
 * hier landen, nicht in der Oberflaeche. Die [Call]-Referenz bleibt im Dienst-Prozess -
 * die Oberflaeche ruft nur die Verben auf.
 */
object InCallRepository {

    private val _call = MutableStateFlow<CallView?>(null)
    val call: StateFlow<CallView?> = _call.asStateFlow()

    private var current: Call? = null

    /** Der zweite Anruf, falls es einen gibt - fuer [switchCall]. */
    private var other: Call? = null
    private var service: BigInCallService? = null

    fun attach(inCallService: BigInCallService) {
        service = inCallService
    }

    fun detach() {
        service = null
        current = null
        other = null
        _call.value = null
    }

    fun publish(call: Call?, view: CallView?, other: Call? = null) {
        current = call
        this.other = other
        _call.value = view
    }

    fun answer() = runCatching { current?.answer(0) }
    fun reject() = runCatching { current?.reject(false, null) }
    fun hangUp() = runCatching { current?.disconnect() }
    fun hold() = runCatching { current?.hold() }
    fun unhold() = runCatching { current?.unhold() }

    /**
     * Zurueck zum gehaltenen Anruf. Das System legt den laufenden dabei selbst auf Halten -
     * beides von Hand zu tun, wuerde die Reihenfolge verlieren, in der es geschieht.
     */
    fun switchCall() = runCatching { other?.unhold() }
    fun playDigit(digit: Char) = runCatching {
        current?.playDtmfTone(digit)
        current?.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) = runCatching { service?.setMuted(muted) }

    fun setSpeaker(on: Boolean) =
        setRoute(if (on) AudioRoute.SPEAKER else AudioRoute.EARPIECE)

    /** Ton auf einen bestimmten Weg legen - Hoermuschel, Lautsprecher oder Bluetooth. */
    fun setRoute(route: AudioRoute) = runCatching {
        service?.setAudioRoute(AudioRoutes.toTelecom(route))
    }
}
