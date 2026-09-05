package org.biglau.phone

import android.telecom.Call
import android.telecom.InCallService
import org.biglau.data.AudioRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * the running call, between service and screen.
 *
 * the [Call] reference stays in the service process; the screen only calls the verbs.
 */
object InCallRepository {

    private val _call = MutableStateFlow<CallView?>(null)
    val call: StateFlow<CallView?> = _call.asStateFlow()

    private var current: Call? = null

    /** the second call, if there is one, for [switchCall]. */
    private var other: Call? = null

    // `InCallService`, not `BigInCallService`: only `setMuted` and `setAudioRoute` are
    // needed, and both come from the system. the own class would tie this to the app module.
    private var service: InCallService? = null

    fun attach(inCallService: InCallService) {
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

    /** back to the held call; the system holds the running one itself, in the right order. */
    fun switchCall() = runCatching { other?.unhold() }

    fun playDigit(digit: Char) = runCatching {
        current?.playDtmfTone(digit)
        current?.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) = runCatching { service?.setMuted(muted) }

    fun setSpeaker(on: Boolean) =
        setRoute(if (on) AudioRoute.SPEAKER else AudioRoute.EARPIECE)

    fun setRoute(route: AudioRoute) = runCatching {
        service?.setAudioRoute(AudioRoutes.toTelecom(route))
    }
}
