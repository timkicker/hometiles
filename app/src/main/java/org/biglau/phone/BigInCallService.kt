package org.biglau.phone

import android.content.Intent
import android.telecom.Call
import org.biglau.data.AudioRoute
import org.biglau.data.ConfigStore
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

    /** Gespraeche, deren Ton schon einmal gestellt wurde - siehe [CallAudio]. */
    private val tonGestellt = mutableSetOf<Call>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            stelleTon(call, state)
            publish(call)
        }
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish(call)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        // Gesperrte Nummer: abweisen, bevor der Bildschirm aufgeht. Sonst klingelt es
        // kurz und der Anrufbildschirm blitzt auf - eine Sperre, die man sieht, ist
        // fuer den Genervten keine.
        val nummer = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (CallBlocking.isBlocked(nummer, ConfigStore.get(this).current.phone.blockedNumbers)) {
            runCatching { call.reject(false, null) }
            return
        }
        InCallRepository.attach(this)
        call.registerCallback(callback)
        publish(call)
        // Vollbild statt einer Benachrichtigung: darum geht es bei dieser App.
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    /**
     * Beim Verbinden einmal den Ton umstellen, wenn die Einstellung es verlangt.
     *
     * Einmal je Gespraech: bei jedem Zustandswechsel nachzuziehen wuerde den Lautsprecher
     * wieder einschalten, den der Nutzer gerade von Hand ausgemacht hat.
     */
    private fun stelleTon(call: Call, state: Int) {
        if (state != Call.STATE_ACTIVE || !tonGestellt.add(call)) return
        val ausgehend = call.details?.callDirection == Call.Details.DIRECTION_OUTGOING
        val weg = CallAudio.routeOnConnect(
            ConfigStore.get(this).current.phone,
            outgoing = ausgehend,
        ) ?: return
        runCatching {
            setAudioRoute(
                when (weg) {
                    AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
                    AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                    AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
                },
            )
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        tonGestellt.remove(call)
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
                // Erst das Adressbuch, dann was das Netz mitschickt. Der Name aus dem
                // Netz (CNAP) kommt in Oesterreich praktisch nie, und ohne ihn stand hier
                // nur eine Ziffernfolge.
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
