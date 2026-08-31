package org.biglau.phone

import android.telecom.Call
import android.telecom.CallAudioState
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
    private var service: BigInCallService? = null

    fun attach(inCallService: BigInCallService) {
        service = inCallService
    }

    fun detach() {
        service = null
        current = null
        _call.value = null
    }

    fun publish(call: Call?, view: CallView?) {
        current = call
        _call.value = view
    }

    fun answer() = runCatching { current?.answer(0) }
    fun reject() = runCatching { current?.reject(false, null) }
    fun hangUp() = runCatching { current?.disconnect() }
    fun hold() = runCatching { current?.hold() }
    fun unhold() = runCatching { current?.unhold() }
    fun playDigit(digit: Char) = runCatching {
        current?.playDtmfTone(digit)
        current?.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) = runCatching { service?.setMuted(muted) }

    fun setSpeaker(on: Boolean) = runCatching {
        service?.setAudioRoute(
            if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
        )
    }
}
