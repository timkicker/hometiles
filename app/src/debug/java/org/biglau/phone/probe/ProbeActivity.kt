package org.biglau.phone.probe

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Stellt den Probeanruf. Nur im Debug-Bau, siehe [Probeanruf].
 *
 *     adb shell am start -n org.biglau.debug/org.biglau.phone.probe.ProbeActivity
 *
 * **Nur eingehend, mit Absicht.** Ein ausgehender Probeanruf ginge ueber
 * `TelecomManager.placeCall`, und dort entscheidet ein Zusatzfeld darueber, ob Telecom die
 * Probe oder das Funkmodul nimmt. Faellt das Feld einmal weg, waehlt das Geraet wirklich.
 * `addNewIncomingCall` kann das nicht: es nennt die `ConnectionService` beim Namen, und
 * eine andere kommt nicht in Frage. Der Anrufbildschirm ist danach ohnehin derselbe -
 * nach dem Annehmen laeuft ein aktives Gespraech mit allem, was dazugehoert.
 */
class ProbeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val telecom = getSystemService(TelecomManager::class.java)
        val konto = PhoneAccountHandle(
            ComponentName(this, ProbeConnectionService::class.java),
            Probeanruf.KONTO,
        )

        val angemeldet = runCatching {
            telecom.registerPhoneAccount(
                PhoneAccount.builder(konto, "BigLau Probeanruf")
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .setShortDescription("Ein Anruf, den niemand fuehrt")
                    .build(),
            )
        }
        if (angemeldet.isFailure) {
            melden("Konto nicht anzumelden: ${angemeldet.exceptionOrNull()}")
            finish()
            return
        }

        val gestellt = runCatching {
            telecom.addNewIncomingCall(
                konto,
                Bundle().apply {
                    putParcelable(
                        TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        Uri.fromParts("tel", Probeanruf.NUMMER, null),
                    )
                    // adb ... -e klingeln ja
                    putBoolean(Probeanruf.EXTRA_KLINGELN, intent?.getStringExtra("klingeln") == "ja")
                    putBoolean(Probeanruf.EXTRA_NICHT_PROTOKOLLIEREN, true)
                },
            )
        }
        // Selbst abheben, damit die Probe nicht laeutet.
        //
        // Drei Versuche gingen davor daneben, alle in der `ConnectionService`:
        // `setDialing()`, `setActive()` bei der Erzeugung, `setActive()` kurz danach.
        // Telecom haelt einen ueber `addNewIncomingCall` gestellten Anruf auf RINGING,
        // bis ihn jemand **annimmt** - und das ist der Weg, den es dafuer gibt.
        if (gestellt.isSuccess && intent?.getStringExtra("klingeln") != "ja") {
            android.os.Handler(mainLooper).postDelayed(
                { runCatching { telecom.acceptRingingCall() } },
                600L,
            )
        }
        melden(
            if (gestellt.isSuccess) {
                "Probeanruf gestellt"
            } else {
                // Der haeufigste Grund: das Konto ist angemeldet, aber nicht freigegeben.
                // Dann sagt Telecom nichts, es passiert nur nichts - deshalb steht der Weg
                // hier und nicht in einer Anleitung woanders.
                "Probeanruf nicht gestellt (Konto freigeben: telecom set-phone-account-enabled)"
            },
        )
        finish()
    }

    /**
     * Nur ins Log, nicht auf den Bildschirm.
     *
     * Die erste Fassung zeigte einen Toast - und der lag beim klingelnden Probeanruf genau
     * ueber dem **Ablehnen**-Knopf. Eine Probe, die einen der zwei Knoepfe verdeckt, um die
     * es geht, prueft den Bildschirm nicht, sondern verstellt ihn.
     */
    private fun melden(text: String) {
        android.util.Log.i("BigLau", text)
    }
}
