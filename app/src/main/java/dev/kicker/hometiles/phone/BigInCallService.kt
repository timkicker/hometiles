package dev.kicker.hometiles.phone

import android.content.Intent
import android.telecom.Call
import dev.kicker.hometiles.data.AudioRoute
import dev.kicker.hometiles.data.ConfigStore
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * takes over the call screen once HomeTiles holds the phone role.
 *
 * the service holds little: it translates the state and passes it on. what decides which
 * buttons appear sits in [CallActions], where it can be tested.
 */
class BigInCallService : InCallService() {

    private var audioState: CallAudioState? = null

    /** calls whose audio has been set once already; see [CallAudio]. */
    private val audioSet = mutableSetOf<Call>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            setAudio(call, state)
            // the notice belongs to the ringing, not to the call: left standing it would sit
            // in the shade through the whole conversation.
            if (state != Call.STATE_RINGING) CallNotifications.clear(this@BigInCallService)
            publish(call)
        }
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish(call)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        // reject before the screen opens, or it rings briefly and the call screen flashes:
        // a block one can see is no block to the person being pestered.
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (CallBlocking.isBlocked(number, ConfigStore.get(this).current.phone.blockedNumbers)) {
            runCatching { call.reject(false, null) }
            return
        }
        InCallRepository.attach(this)
        call.registerCallback(callback)
        publish(call)
        // a ringing call goes through the notice: with the screen off this service is in the
        // background, where android refuses the direct start. see CallNotifications.
        if (call.state == Call.STATE_RINGING) {
            CallNotifications.showIncoming(
                this,
                name = CallerName.lookup(this, number),
                number = number,
            )
            return
        }
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    /**
     * set the route once on connecting, if the setting asks for it. once per call: following
     * every state change would switch the speaker back on right after the user turned it off.
     */
    private fun setAudio(call: Call, state: Int) {
        if (state != Call.STATE_ACTIVE || !audioSet.add(call)) return
        val outgoing = call.details?.callDirection == Call.Details.DIRECTION_OUTGOING
        val route = CallAudio.routeOnConnect(
            ConfigStore.get(this).current.phone,
            outgoing = outgoing,
        ) ?: return
        runCatching { setAudioRoute(AudioRoutes.toTelecom(route)) }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        audioSet.remove(call)
        call.unregisterCallback(callback)
        CallNotifications.clear(this)
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

    /** a conference member; telecom reports it as a call of its own beside the conference. */
    private fun Call.isConferenceMember(): Boolean = parent != null

    /** which call is shown; the order sits in [CallForeground] and is checked there. */
    private fun foreground(): Call? {
        val list = calls
        val index = CallForeground.pick(
            list.map { statusOf(it.state) },
            list.map { it.isConferenceMember() },
        ) ?: return null
        return list.getOrNull(index)
    }

    private fun publish(call: Call) {
        val shown = foreground() ?: call
        if (shown != call) return publish(shown)
        val second = CallForeground
            .other(calls.map { it.isConferenceMember() }, calls.indexOf(call))
            ?.let { calls.getOrNull(it) }
        val details = call.details
        InCallRepository.publish(
            call = call,
            view = CallView(
                status = statusOf(call.state),
                number = details?.handle?.schemeSpecificPart.orEmpty(),
                // the phone book first, then what the network sends: CNAP practically
                // never arrives here, and without it only digits stood there.
                name = CallerName.lookup(
                    this,
                    details?.handle?.schemeSpecificPart.orEmpty(),
                ) ?: details?.callerDisplayName?.takeIf { it.isNotBlank() },
                photoUri = CallerName.photo(
                    this,
                    details?.handle?.schemeSpecificPart.orEmpty(),
                ),
                startedAtMillis = details?.connectTimeMillis?.takeIf { it > 0 },
                muted = audioState?.isMuted == true,
                audioRoute = AudioRoutes.fromTelecom(audioState?.route),
                bluetoothAvailable = AudioRoutes.bluetoothAvailable(
                    audioState?.supportedRouteMask,
                ),
                otherName = second?.let {
                    val other = it.details?.handle?.schemeSpecificPart.orEmpty()
                    CallerName.lookup(this, other) ?: other.takeIf { n -> n.isNotBlank() }
                },
                otherHeld = second?.state == Call.STATE_HOLDING,
            ),
            other = second,
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
