package org.biglau.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * Uebernimmt die Gespraechsansicht, sobald BigLau die Telefon-Rolle haelt.
 *
 * Der Dienst haelt bewusst wenig: er uebersetzt den Zustand und reicht ihn weiter. Alles,
 * was entscheidet, welche Knoepfe erscheinen, steht in [CallActions] und ist dort geprueft -
 * hier waere es weder testbar noch zu ueberblicken.
 */
class BigInCallService : InCallService() {

    private var audioState: CallAudioState? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish(call)
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish(call)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        InCallRepository.attach(this)
        call.registerCallback(callback)
        publish(call)
        // Vollbild statt einer Benachrichtigung: darum geht es bei dieser App.
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        if (calls.isEmpty()) {
            InCallRepository.detach()
        } else {
            publish(calls.first())
        }
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        audioState = state
        calls.firstOrNull()?.let(::publish)
    }

    private fun publish(call: Call) {
        val details = call.details
        InCallRepository.publish(
            call = call,
            view = CallView(
                status = statusOf(call.state),
                number = details?.handle?.schemeSpecificPart.orEmpty(),
                name = details?.callerDisplayName?.takeIf { it.isNotBlank() },
                startedAtMillis = details?.connectTimeMillis?.takeIf { it > 0 },
                muted = audioState?.isMuted == true,
                speakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER,
                otherCallWaiting = calls.size > 1,
            ),
        )
    }

    private fun statusOf(state: Int): CallStatus = when (state) {
        Call.STATE_RINGING -> CallStatus.RINGING
        Call.STATE_DIALING -> CallStatus.DIALING
        Call.STATE_CONNECTING -> CallStatus.CONNECTING
        Call.STATE_ACTIVE -> CallStatus.ACTIVE
        Call.STATE_HOLDING -> CallStatus.HOLDING
        Call.STATE_DISCONNECTING -> CallStatus.DISCONNECTING
        Call.STATE_DISCONNECTED -> CallStatus.DISCONNECTED
        else -> CallStatus.OTHER
    }
}
