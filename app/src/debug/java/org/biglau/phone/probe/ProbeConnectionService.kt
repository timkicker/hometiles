package org.biglau.phone.probe

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
 * Telecoms Gegenueber fuer den Probeanruf. Siehe [Probeanruf].
 *
 * `audioModeIsVoip = true` ist der Kern: damit fuehrt Telecom den Ton ueber den
 * gewoehnlichen Audioweg statt ueber das Funkmodul - und genau deshalb laesst sich der
 * Wechsel zwischen Hoermuschel, Lautsprecher und Bluetooth hier wirklich pruefen und nicht
 * nur ansehen.
 */
class ProbeConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = verbindung(
        request,
        // Klingeln ist die Ausnahme, nicht die Vorgabe. Ein Probeanruf, der um halb zehn
        // abends laeutet, prueft den Anrufbildschirm und weckt das Haus. Wer das Klingeln
        // pruefen will, sagt es ausdruecklich - und waehlt die Zeit selbst.
        klingeln = request?.extras?.getBoolean(Probeanruf.EXTRA_KLINGELN) == true,
    )

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = verbindung(request, klingeln = false)

    private fun verbindung(request: ConnectionRequest?, klingeln: Boolean): Connection {
        val nummer = request?.address?.schemeSpecificPart ?: Probeanruf.NUMMER
        if (!Probeanruf.erlaubt(nummer)) {
            // Hier endet die Probe, bevor sie anfaengt. Ein Probeanruf auf eine
            // Notrufnummer waere kein Probeanruf mehr.
            Log.w("BigLau", "Probeanruf abgelehnt: $nummer")
            return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, "keine Probe mit dieser Nummer"),
            )
        }
        return ProbeVerbindung(applicationContext).apply {
            setAddress(
                Uri.fromParts("tel", nummer, null),
                TelecomManager.PRESENTATION_ALLOWED,
            )
            setCallerDisplayName(Probeanruf.NAME, TelecomManager.PRESENTATION_ALLOWED)
            connectionCapabilities = Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_HOLD or
                Connection.CAPABILITY_SUPPORT_HOLD
            audioModeIsVoip = true
            // **Ohne Klingelphase.** Zwei Versuche am 03.09.2026: `setDialing()` und
            // dann `setActive()` bei der Erzeugung - beide wirkungslos, weil Telecom einen
            // ueber `addNewIncomingCall` gestellten Anruf auf RINGING setzt und dort
            // wartet, bis jemand abhebt. Der dritte Weg hebt selbst ab, kurz nachdem
            // Telecom fertig ist. Zwei Probeanrufe standen dabei je vier Sekunden auf
            // RINGING; hoerbar war das nicht, weil auf diesem Geraet STREAM_RING stumm
            // geschaltet ist - aber darauf soll sich eine Probe nicht verlassen.
            setRinging()
            if (!klingeln) selbstAbheben()
            selbstBeenden()
        }
    }
}

/** Der Anruf selbst. Jede Taste im Anrufbildschirm landet hier. */
private class ProbeVerbindung(private val context: Context) : Connection() {

    /**
     * Nach zwei Minuten legt die Probe von selbst auf.
     *
     * Ein echtes Gespraech endet, weil jemand auflegt. Eine Probe endet, weil jemand daran
     * denkt - und wenn nicht, steht sie in der Statusleiste, bis das Telefon neu startet.
     * Der Countdown laeuft im Hauptthread, wo Telecom die Verbindung ohnehin fuehrt.
     */
    /** Hebt gleich selbst ab, damit gar nicht erst geklingelt wird. */
    fun selbstAbheben() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            // Nicht `state == STATE_RINGING` abfragen: 250 ms nach der Erzeugung stand
            // die Verbindung noch auf STATE_NEW, die Bedingung traf nie zu, und der Anruf
            // klingelte weiter. Gefragt ist nur, dass er noch da ist.
            { if (state != STATE_DISCONNECTED) setActive() },
            600L,
        )
    }

    fun selbstBeenden() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { if (state != STATE_DISCONNECTED) beenden(DisconnectCause.LOCAL) },
            120_000L,
        )
    }

    override fun onAnswer() {
        setActive()
    }

    override fun onReject() {
        beenden(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        beenden(DisconnectCause.LOCAL)
    }

    override fun onAbort() {
        beenden(DisconnectCause.CANCELED)
    }

    override fun onHold() {
        setOnHold()
    }

    override fun onUnhold() {
        setActive()
    }

    /**
     * Die Tastentoene bleiben still.
     *
     * Im Ernstfall spielt sie das Netz, nicht das Geraet. Hier gaebe es niemanden, der sie
     * hoert - ausser dem Menschen im Zimmer, und der hat nachts nichts davon.
     */
    override fun onPlayDtmfTone(c: Char) {
        Log.i("BigLau", "Probeanruf DTMF: $c")
    }

    fun beenden(grund: Int) {
        setDisconnected(DisconnectCause(grund))
        destroy()
        aufraeumen()
    }

    /**
     * Die Probe raeumt ihre Spur aus der Anrufliste.
     *
     * Am 03.09.2026 standen nach sieben Proben „+44 7700 900123 (7)“ oben in der
     * Anrufliste - vor „zhenya“ und den echten verpassten Anrufen. Telecom schreibt den
     * Eintrag selbst, und die Bitte `android.telecom.extra.DO_NOT_LOG_CALL` wird auf
     * Android 11 nicht beachtet (am Geraet nachgesehen: der Zaehler ging trotzdem von 6
     * auf 7). Also wird hinterher geloescht, und zwar erst nach zwei Sekunden - vorher
     * steht der Eintrag noch gar nicht da.
     *
     * Nur diese eine Nummer, und die ist fuer Film und Fernsehen reserviert: es kann
     * keinen echten Anruf geben, der hier mitgeloescht wird.
     */
    private fun aufraeumen() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            {
                runCatching {
                    val weg = context.contentResolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        "${CallLog.Calls.NUMBER} LIKE ?",
                        arrayOf("%" + Probeanruf.NUMMER.takeLast(10)),
                    )
                    Log.i("BigLau", "Probeanruf aus der Anrufliste geraeumt: $weg")
                }.onFailure { Log.w("BigLau", "Anrufliste nicht geraeumt: $it") }
            },
            2_000L,
        )
    }
}
