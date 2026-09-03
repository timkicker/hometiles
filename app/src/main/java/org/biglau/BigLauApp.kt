package org.biglau

import android.app.Application
import org.biglau.sms.SmsReminder
import org.biglau.sms.MessageReminderReceiver
import org.biglau.apps.AppRepository
import org.biglau.data.ConfigStore
import org.biglau.phone.SystemNumbers
import org.biglau.safety.CrashRecorder

class BigLauApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Zuerst und auf diesem Faden: Abstuerze festhalten, damit der Notmodus etwas
        // anzuzeigen hat. Das kostet zwei Millisekunden.
        CrashRecorder.get(this).installHandler()

        // Die Einrichtung einlesen kostet am Jelly 2 **186 ms** - nicht die Datei (zwoelf
        // Kilobyte), sondern das erste Benutzen des Umwandlers. Auf diesem Faden liegt das
        // vor allem anderen; daneben laeuft es waehrend des Startens der Activity mit.
        //
        // Gefahrlos, weil `ConfigStore.get` ohnehin gegen zwei gleichzeitige Aufrufe
        // gesichert ist: kommt die Oberflaeche frueher, wartet sie eben - genauso lange wie
        // vorher, keinen Augenblick laenger.
        Thread {
            ConfigStore.get(this)
            AppRepository.get(this)
            // Schreibweise und Land fuer Nummern ohne Vorwahl. Ohne diese Auskunft
            // schreibt BigLau Rufnummern in blossen Dreierbloecken, und die
            // Laendervorwahl klebt am Ortsnetz - siehe PhoneNumbers.forDisplay.
            // Braucht keine Berechtigung.
            SystemNumbers.install(this)
            // Den Wecker fuer die wiederholte Erinnerung wieder stellen.
            //
            // Er haengt an `ELAPSED_REALTIME_WAKEUP` und ist nach einem Neustart des
            // Telefons weg - ein `BOOT_COMPLETED`-Empfaenger stuende dafuer im Manifest und
            // braeuchte eine weitere Berechtigung. Die braucht es nicht: BigLau **ist** der
            // Startbildschirm und laeuft nach jedem Neustart ohnehin. Ohne diese Zeile
            // erinnerte eine Nachricht, die vor dem Neustart ungelesen war, nie wieder -
            // lautlos, und das ist genau das, was die Einstellung verspricht.
            //
            // Gefahrlos, wenn nichts ansteht: `MessageReminderReceiver` prueft selbst, ob
            // die Erinnerung an ist, ob BigLau die Standard-SMS-App ist und ob ueberhaupt
            // etwas ungelesen ist - sonst tut der Weckruf nichts und stellt sich auch nicht
            // neu.
            val sms = ConfigStore.get(this).current.sms
            if (SmsReminder.active(sms)) {
                MessageReminderReceiver.schedule(this, sms.repeatMinutes)
            }
        }.start()
    }
}
