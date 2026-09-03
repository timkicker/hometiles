package org.biglau.phone

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager

/**
 * Die Systemteile von [PhoneNumbers] - die einzige Stelle, die dafuer Android anfasst.
 *
 * [PhoneNumbers] selbst rechnet nur und liegt deshalb in `core:model`, wo der Compiler
 * "kein Android" erzwingt. Zwei Auskuenfte kann es dort aber nicht selbst holen: die
 * Schreibweise einer Nummer (Android bringt die Vorwahltabellen mit) und das Land der SIM.
 * Beide haengt [install] beim Start ein.
 *
 * Ohne diesen Aufruf schreibt BigLau Nummern in blossen Dreierbloecken. Das ist kein
 * Absturz, sondern ein leiser Rueckschritt - genau die Sorte Fehler, die niemand meldet.
 * Deshalb prueft `StartAufgabenTest`, dass der Aufruf beim Start steht.
 */
object SystemNumbers {

    fun install(context: Context) {
        PhoneNumbers.systemFormat = { nummer, land ->
            runCatching { PhoneNumberUtils.formatNumber(nummer, land) }.getOrNull()
        }
        // Braucht keine Berechtigung: die Landeskennung der SIM ist frei lesbar.
        PhoneNumbers.region = runCatching {
            context.getSystemService(TelephonyManager::class.java)
                ?.simCountryIso
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
