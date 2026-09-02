package org.biglau.notify

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction

/**
 * Welche Standard-Apps das System gerade verwendet. Null heisst: keine gesetzt oder
 * nicht ermittelbar - dann blinkt die zugehoerige Kachel eben nicht.
 */
data class SystemPackages(
    val sms: String? = null,
    val dialer: String? = null,
)

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

    /** Anzahl fuer diese Kachel; null oder abgeschaltet ergibt null Treffer. */
    fun badgeFor(
        button: Button,
        counts: Map<String, Int>,
        system: SystemPackages,
    ): Int {
        if (!button.blink) return 0
        val watched = watchedPackage(button.action, system) ?: return 0
        return counts[watched] ?: 0
    }
}
