package org.biglau.phone

import org.biglau.data.AudioRoute

/** Der Zustand eines Anrufs, so weit die Oberflaeche ihn braucht. */
enum class CallStatus { RINGING, DIALING, CONNECTING, ACTIVE, HOLDING, DISCONNECTING, DISCONNECTED, OTHER }

/** Was der Nutzer gerade tun kann. */
enum class CallAction {
    ANSWER,
    REJECT,
    HANG_UP,
    HOLD,
    UNHOLD,
    MUTE,
    UNMUTE,
    SPEAKER,

    /**
     * Lautsprecher wieder aus.
     *
     * **Den gab es nicht** - "Lautsprecher" schaltete ihn nur ein, ein zweiter Druck tat
     * dasselbe noch einmal. Wer ihn versehentlich anschaltete, bekam ihn bis zum Auflegen
     * nicht mehr weg, und das Gespraech lief derweil laut durch den Raum.
     */
    SPEAKER_OFF,

    /**
     * Wohin der Ton geht - als Auswahl.
     *
     * Steht an der Stelle von [SPEAKER], **sobald ein Bluetooth-Geraet da ist**. Mit drei
     * Moeglichkeiten reicht ein Umschalter nicht mehr, und eine vierte Knopfzeile passt auf
     * drei Zoll nicht dazu: fuenf Zeilen fuellen den Bildschirm schon. Also zeigt die Zeile
     * den derzeitigen Weg an und fuehrt zur Auswahl. Ohne Bluetooth bleibt es beim
     * Umschalter - ein zusaetzlicher Schritt fuer den haeufigen Fall waere ein schlechter
     * Tausch.
     */
    AUDIO,

    /**
     * Zum zweiten, gehaltenen Anruf wechseln.
     *
     * Steht an der Stelle von [HOLD]: solange noch jemand in der Leitung wartet, ist
     * "Halten" die falsche Frage. Am Emulator gesehen, dass der gehaltene Anruf sonst
     * **unerreichbar** war - er stand nirgends, und kein Knopf fuehrte zurueck.
     */
    SWITCH,
    KEYPAD,
}

data class CallView(
    val status: CallStatus,
    val number: String,
    val name: String?,
    /** Bildadresse des Kontaktfotos, sofern es eines gibt. `PLAN.md` 4.6. */
    val photoUri: String? = null,
    val startedAtMillis: Long?,
    val muted: Boolean = false,
    /** Wohin der Ton derzeit geht. */
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    /** Ist ein Bluetooth-Geraet verbunden? Dann wird aus dem Umschalter eine Auswahl. */
    val bluetoothAvailable: Boolean = false,
    /**
     * Name (oder Nummer) des zweiten Anrufs, falls es einen gibt.
     *
     * Vorher stand hier `otherCallWaiting: Boolean` - gesetzt, aber nirgends gelesen. Ein
     * zweiter Anruf ging damit spurlos an der Oberflaeche vorbei: waehrend eines
     * Gespraechs klingelte es, der Bildschirm zeigte nur den neuen Anrufer, und dass
     * nebenan noch jemand in der Leitung war, stand nirgends.
     */
    val otherName: String? = null,
    /** Der zweite Anruf wird gehalten - dann fuehrt [CallAction.SWITCH] zu ihm zurueck. */
    val otherHeld: Boolean = false,
)

/**
 * Welcher von mehreren Anrufen im Vordergrund steht.
 *
 * Vorher wurde der zuletzt veraenderte gezeigt - also mal der eine, mal der andere. Die
 * Reihenfolge hier ist die der Entscheidung, die ansteht: es klingelt (annehmen oder
 * nicht?) vor dem laufenden Gespraech vor dem gehaltenen.
 */
object CallForeground {

    fun pick(states: List<CallStatus>): Int? = when {
        states.isEmpty() -> null
        else -> states.indexOfFirst { it == CallStatus.RINGING }
            .takeIf { it >= 0 }
            ?: states.indexOfFirst { it == CallStatus.ACTIVE }.takeIf { it >= 0 }
            ?: 0
    }
}

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
            tonZeile(view),
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
        )

        CallStatus.ACTIVE -> listOf(
            CallAction.HANG_UP,
            if (view.muted) CallAction.UNMUTE else CallAction.MUTE,
            tonZeile(view),
            if (view.otherHeld) CallAction.SWITCH else CallAction.HOLD,
            CallAction.KEYPAD,
        )

        CallStatus.HOLDING -> listOf(CallAction.HANG_UP, CallAction.UNHOLD)

        CallStatus.DISCONNECTING, CallStatus.DISCONNECTED, CallStatus.OTHER -> emptyList()
    }

    /**
     * Welche Knopfzeilen neben der geoeffneten Tastatur stehenbleiben.
     *
     * **Anlass, am Emulator gesehen:** mit allen fuenf Zeilen blieb fuer die Tastatur ein
     * Streifen von fuenf dp - zwoelf Bildpunkte, ohne dass die Ziffern ueberhaupt noch
     * gezeichnet wurden. Wer im Gespraech eine Nummer eingeben muss ("fuer Deutsch die 1"),
     * kam nicht ans Ziel.
     *
     * Bleiben duerfen zwei: Auflegen, weil es das Wichtigste ist, und die Tastatur selbst,
     * damit der Weg zurueck sichtbar bleibt - vorher schloss sie nur die Ruecktaste, was
     * niemand ahnen kann.
     */
    fun whileKeypad(view: CallView): List<CallAction> =
        availableFor(view).filter { it == CallAction.HANG_UP || it == CallAction.KEYPAD }

    /** Die Zeile fuer den Ton: Umschalter, oder Auswahl, sobald Bluetooth da ist. */
    private fun tonZeile(view: CallView): CallAction = when {
        view.bluetoothAvailable -> CallAction.AUDIO
        view.audioRoute == AudioRoute.SPEAKER -> CallAction.SPEAKER_OFF
        else -> CallAction.SPEAKER
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

    /**
     * Was gross ueber dem Anruf steht: Name, sonst Nummer, sonst [unbekannt].
     *
     * Hier stand ein festes `"?"` - auf Deutsch wie auf Englisch, und ohne jede Aussage.
     * Bei einer unterdrueckten Nummer, also einem der haeufigsten Faelle ueberhaupt, fuellte
     * ein Fragezeichen den halben Bildschirm; das sieht aus wie ein Fehler der App und nicht
     * wie eine Auskunft ueber den Anrufer.
     */
    fun headline(view: CallView, unbekannt: String): String =
        view.name?.takeIf { it.isNotBlank() }
            ?: PhoneNumbers.forDisplay(view.number).takeIf { it.isNotBlank() }
            ?: unbekannt
}
