package org.biglau.phone

/** Der Zustand eines Anrufs, so weit die Oberflaeche ihn braucht. */
enum class CallStatus { RINGING, DIALING, CONNECTING, ACTIVE, HOLDING, DISCONNECTING, DISCONNECTED, OTHER }

/** Was der Nutzer gerade tun kann. */
enum class CallAction { ANSWER, REJECT, HANG_UP, HOLD, UNHOLD, MUTE, UNMUTE, SPEAKER, KEYPAD }

data class CallView(
    val status: CallStatus,
    val number: String,
    val name: String?,
    /** Bildadresse des Kontaktfotos, sofern es eines gibt. `PLAN.md` 4.6. */
    val photoUri: String? = null,
    val startedAtMillis: Long?,
    val muted: Boolean = false,
    val speakerOn: Boolean = false,
    val otherCallWaiting: Boolean = false,
)

/**
 * Welche Knoepfe in welchem Zustand erscheinen.
 *
 * Das ist die Stelle, an der ein Fehler richtig wehtut: ein "Auflegen" auf einem klingelnden
 * Anruf sieht aus wie "Ablehnen", ein fehlendes "Annehmen" macht das Telefon unbrauchbar.
 * Deshalb steht es als Tabelle hier und nicht verteilt in der Oberflaeche.
 */
object CallActions {

    fun availableFor(view: CallView): List<CallAction> = when (view.status) {
        CallStatus.RINGING -> listOf(CallAction.ANSWER, CallAction.REJECT)

        CallStatus.DIALING, CallStatus.CONNECTING -> listOf(
            CallAction.HANG_UP,
            if (view.speakerOn) CallAction.SPEAKER else CallAction.SPEAKER,
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
        )

        CallStatus.ACTIVE -> listOf(
            CallAction.HANG_UP,
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
            CallAction.SPEAKER,
            CallAction.HOLD,
            CallAction.KEYPAD,
        )

        CallStatus.HOLDING -> listOf(CallAction.HANG_UP, CallAction.UNHOLD)

        CallStatus.DISCONNECTING, CallStatus.DISCONNECTED, CallStatus.OTHER -> emptyList()
    }

    /** Gespraechsdauer in Sekunden; null, solange nicht verbunden. */
    fun durationSeconds(view: CallView, nowMillis: Long): Long? {
        val started = view.startedAtMillis ?: return null
        if (view.status != CallStatus.ACTIVE && view.status != CallStatus.HOLDING) return null
        return ((nowMillis - started) / 1000L).coerceAtLeast(0L)
    }

    /** mm:ss, ab einer Stunde h:mm:ss. */
    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
        else "%d:%02d".format(minutes, secs)
    }

    /** Was gross auf dem Bildschirm steht. */
    fun headline(view: CallView): String =
        view.name?.takeIf { it.isNotBlank() }
            ?: PhoneNumbers.forDisplay(view.number).takeIf { it.isNotBlank() }
            ?: "?"
}
