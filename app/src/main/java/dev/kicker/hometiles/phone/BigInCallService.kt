package dev.kicker.hometiles.phone

import android.content.Intent
import android.os.Build
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

    /** calls that were dialled from here, on the androids that do not say so; see [noteDirection]. */
    private val dialledOut = mutableSetOf<Call>()

    /** calls that rang here and have not been taken yet. */
    private val ringing = mutableSetOf<Call>()

    private val proximity by lazy { ProximityLock(this) }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            setAudio(call, state)
            // the notice belongs to the ringing, not to the call: left standing it would sit
            // in the shade through the whole conversation.
            if (state != Call.STATE_RINGING) CallNotifications.clear(this@BigInCallService)
            publish(call)
            // taken without the call screen - from the notice, a headset button, the green
            // key - the call ran with no screen and no notice, and nothing to hang up with
            // (issue #2, probe call on the emulator 25.10.2026).
            if (state == Call.STATE_ACTIVE && ringing.remove(call)) openCallScreen()
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
        noteDirection(call)
        InCallRepository.attach(this)
        call.registerCallback(callback)
        publish(call)
        // a ringing call goes through the notice: with the screen off android refuses the
        // direct start, see CallNotifications. with the screen on the notice stays small, so
        // the call screen is also opened directly; where android refuses that too, the
        // notice carries answer and reject.
        if (call.state == Call.STATE_RINGING) {
            ringing.add(call)
            CallNotifications.showIncoming(
                this,
                name = CallerName.lookup(this, number),
                number = number,
            )
        }
        openCallScreen()
    }

    /** singleTask: a screen that is already open comes to the front and keeps its state. */
    private fun openCallScreen() {
        runCatching {
            startActivity(
                Intent(this, InCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
    }

    /**
     * set the route once on connecting, if the setting asks for it. once per call: following
     * every state change would switch the speaker back on right after the user turned it off.
     */
    private fun setAudio(call: Call, state: Int) {
        if (state != Call.STATE_ACTIVE || !audioSet.add(call)) return
        val outgoing = isOutgoing(call)
        val route = CallAudio.routeOnConnect(
            ConfigStore.get(this).current.phone,
            outgoing = outgoing,
        ) ?: return
        runCatching { setAudioRoute(AudioRoutes.toTelecom(route)) }
    }

    /**
     * [Call.Details.getCallDirection] arrived with android 10. the state a call carries when
     * it turns up says the same thing: one that was dialled starts out connecting, one that
     * comes in starts out ringing. it has to be read here, because by the time the audio
     * route is set the call is long since active and the starting state is gone.
     */
    private fun noteDirection(call: Call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        if (call.state == Call.STATE_CONNECTING || call.state == Call.STATE_DIALING) {
            dialledOut.add(call)
        }
    }

    private fun isOutgoing(call: Call): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details?.callDirection == Call.Details.DIRECTION_OUTGOING
        } else {
            call in dialledOut
        }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        audioSet.remove(call)
        dialledOut.remove(call)
        ringing.remove(call)
        call.unregisterCallback(callback)
        CallNotifications.clear(this)
        if (calls.isEmpty()) {
            proximity.hold(false)
            InCallRepository.detach()
        } else {
            publish(calls.first())
        }
    }

    /** telecom can unbind with a call still listed; the screen must not stay off without us. */
    override fun onDestroy() {
        proximity.hold(false)
        super.onDestroy()
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
        // before telecom names a route the call runs on the earpiece, which is where a phone
        // starts; a wired headset counts as away from the ear.
        val route = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        proximity.hold(
            CallProximity.screenOff(statusOf(call.state), earpiece = route == CallAudioState.ROUTE_EARPIECE),
        )
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
