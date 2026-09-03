package org.biglau.notify

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction

/**
 * Welches Paket eine Kachel beobachtet.
 *
 * Eine App-Kachel beobachtet ihre eigene App. Die Kacheln "Telefon" und "Nachrichten" zeigen
 * dagegen auf das, was das System gerade als Standard fuehrt - wechselt der Nutzer seine
 * SMS-App, blinkt die Kachel weiter richtig, ohne dass er etwas umstellen muss.
 */
object TileNotifications {

    fun watchedPackage(action: ButtonAction, system: SystemPackages): String? = when (action) {
        is ButtonAction.App -> action.packageName
        is ButtonAction.Action -> when (action.builtin) {
            Builtin.MESSAGES -> system.sms
            Builtin.DIALER -> system.dialer
            // MISSED_CALLS steht bewusst nicht hier: die zaehlt die Anrufliste, nicht
            // fremde Meldungen. Siehe badgeFor.
            else -> null
        }
        else -> null
    }

    /**
     * Kann diese Kachel ueberhaupt blinken?
     *
     * Nur, wenn hinter ihr eine App steckt, die benachrichtigen kann. Einen Schalter fuer
     * eine Uhr oder eine leere Kachel anzubieten hiesse, etwas zu versprechen, das nie
     * eintritt - und der Nutzer suchte den Fehler dann bei sich.
     */
    fun canBlink(action: ButtonAction): Boolean = when (action) {
        is ButtonAction.App -> true
        is ButtonAction.Action -> action.builtin in setOf(
            Builtin.DIALER,
            Builtin.MESSAGES,
            Builtin.MISSED_CALLS,
        )
        else -> false
    }

    /**
     * Anzahl fuer diese Kachel; null oder abgeschaltet ergibt null Treffer.
     *
     * **Verpasste Anrufe zaehlen anders**, und das war ein Fehler: die Kachel sah auf die
     * Meldungen der Standard-Telefon-App - und das ist BigLau selbst, sobald sie die Rolle
     * hat. Die Meldung ueber einen verpassten Anruf kommt aber vom System (Telecom), nicht
     * von der Telefon-App. Am Emulator standen **elf ungesehene verpasste Anrufe** in der
     * Liste, und auf der Kachel stand nichts.
     *
     * Deshalb zaehlt sie jetzt die Anrufliste selbst - das ist ohnehin die Quelle, die der
     * Nutzer meint, und sie braucht keinen Zugriff auf fremde Meldungen.
     */
    fun badgeFor(
        button: Button,
        counts: Map<String, Int>,
        system: SystemPackages,
        missed: Int = 0,
        unread: Int? = null,
    ): Int {
        if (!button.blink) return 0
        val action = button.action
        if (action is ButtonAction.Action) {
            if (action.builtin == Builtin.MISSED_CALLS) return missed
            // Ungelesene Nachrichten weiss der Anbieter genauer als die Meldungen. Null
            // heisst: BigLau darf nicht lesen - dann bleiben die Meldungen die beste
            // Auskunft, die es gibt.
            if (action.builtin == Builtin.MESSAGES && unread != null) return unread
        }
        val watched = watchedPackage(action, system) ?: return 0
        return counts[watched] ?: 0
    }
}
