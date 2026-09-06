package dev.kicker.hometiles.phone.probe

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * telecom's counterpart for the probe call. see [ProbeCall].
 *
 * `audioModeIsVoip = true` is the core: with it telecom carries the sound over the ordinary
 * audio path instead of the radio - which is why the switch between earpiece, speaker and
 * bluetooth can really be checked here and not just looked at.
 */
class ProbeConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = connection(
        request,
        // ringing is the exception, not the default: a probe that rings at half past nine in
        // the evening checks the call screen and wakes the house.
        ringing = request?.extras?.getBoolean(ProbeCall.EXTRA_RINGING) == true,
    )

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = connection(request, ringing = false)

    private fun connection(request: ConnectionRequest?, ringing: Boolean): Connection {
        val number = request?.address?.schemeSpecificPart ?: ProbeCall.NUMBER
        if (!ProbeCall.allowed(number)) {
            Log.w("HomeTiles", "probe call refused: $number")
            return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, "no probe with this number"),
            )
        }
        return ProbeConnection(applicationContext).apply {
            setAddress(
                Uri.fromParts("tel", number, null),
                TelecomManager.PRESENTATION_ALLOWED,
            )
            setCallerDisplayName(ProbeCall.NAME, TelecomManager.PRESENTATION_ALLOWED)
            // no merge capability, so telecom offers no conference: two probes stay two
            // calls. tried on this device on 05.09.2026 - a second probe arrived as an
            // additional call, but a conference cannot be reached this way, and the
            // conference handling in CallForeground is checked by its rules only.
            connectionCapabilities = Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_HOLD or
                Connection.CAPABILITY_SUPPORT_HOLD
            audioModeIsVoip = true
            // two tries on 03.09.2026: `setDialing()` and `setActive()` at creation - both
            // without effect, because telecom puts a call placed through `addNewIncomingCall`
            // on RINGING and waits there until somebody picks up. so the third way picks up
            // itself, shortly after telecom is done.
            setRinging()
            if (!ringing) answerSelf()
            endSelf()
        }
    }
}

/** the call itself. every key on the call screen lands here. */
private class ProbeConnection(private val context: Context) : Connection() {

    /** picks up straight away, so that it does not start ringing at all. */
    fun answerSelf() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            // not `state == STATE_RINGING`: 250 ms after creation the connection still stood
            // on STATE_NEW, the condition never held, and the call kept ringing.
            { if (state != STATE_DISCONNECTED) setActive() },
            600L,
        )
    }

    /**
     * after two minutes the probe hangs up by itself.
     *
     * a real call ends because somebody hangs up. a probe ends because somebody remembers -
     * and if not, it stands in the status bar until the phone restarts.
     */
    fun endSelf() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { if (state != STATE_DISCONNECTED) end(DisconnectCause.LOCAL) },
            120_000L,
        )
    }

    override fun onAnswer() {
        setActive()
    }

    override fun onReject() {
        end(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        end(DisconnectCause.LOCAL)
    }

    override fun onAbort() {
        end(DisconnectCause.CANCELED)
    }

    override fun onHold() {
        setOnHold()
    }

    override fun onUnhold() {
        setActive()
    }

    /** in earnest the network plays the key tones, not the device. here nobody would hear them. */
    override fun onPlayDtmfTone(c: Char) {
        Log.i("HomeTiles", "probe call DTMF: $c")
    }

    fun end(cause: Int) {
        setDisconnected(DisconnectCause(cause))
        destroy()
        cleanUp()
    }

    /**
     * the probe clears its own trace from the call log.
     *
     * on the user's device on 03.09.2026 seven probes stood on top of the call log, above the
     * real missed calls. telecom writes the entry itself, and the request
     * `android.telecom.extra.DO_NOT_LOG_CALL` is ignored on android 11 (looked up at the
     * device: the counter went from 6 to 7 anyway). so it is deleted afterwards, and only
     * after two seconds - before that the entry is not there yet.
     *
     * only this one number, and it is reserved for film and television: there can be no real
     * call that gets deleted along with it.
     */
    private fun cleanUp() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            {
                runCatching {
                    val removed = context.contentResolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        "${CallLog.Calls.NUMBER} LIKE ?",
                        arrayOf("%" + ProbeCall.NUMBER.takeLast(10)),
                    )
                    Log.i("HomeTiles", "probe call cleared from the call log: $removed")
                }.onFailure { Log.w("HomeTiles", "call log not cleared: $it") }
            },
            2_000L,
        )
    }
}
