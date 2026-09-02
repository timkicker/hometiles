package org.biglau.phone

import android.content.Context
import android.telecom.TelecomManager

/**
 * Hält BigLau die Telefon-Rolle?
 *
 * Daran hängt mehr, als es aussieht: **nur die Standard-Telefon-App bekommt eingehende
 * Anrufe zu sehen** und kann sie abweisen. Ohne die Rolle wirkt die Nummernsperre nur nach
 * außen — aus BigLau heraus lässt sich eine gesperrte Nummer nicht wählen, aber wer anruft,
 * klingelt trotzdem durch die Telefon-App des Systems.
 *
 * Genau das stand als Zusage in den Einstellungen („Anrufe von diesen Nummern werden
 * abgewiesen, ohne zu klingeln"), ohne dass jemand nachgesehen hätte. Auf dem Telefon des
 * Nutzers hält die Rolle ein anderes Programm — dort war der Satz falsch.
 */
object DialerRole {

    fun held(context: Context): Boolean =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == context.packageName
}
