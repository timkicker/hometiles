package org.biglau.apps

import org.biglau.data.AppsConfig
import org.biglau.data.ButtonAction
import org.biglau.data.LauncherConfig
import org.biglau.security.Pin

/**
 * Die App-Sperre. PLAN.md 4.5: „welche Apps ohne PIN startbar sind (Whitelist)".
 *
 * Gedacht fuer den Fall, in dem jemand ein Telefon fuer eine andere Person einrichtet -
 * Angehoerige, Pflege - und will, dass nur ein paar Apps offenstehen. Deshalb eine
 * Erlaubnisliste und keine Sperrliste: eine Sperrliste muesste jede App des Telefons
 * nennen und waere nach der naechsten Installation schon unvollstaendig.
 *
 * Genau darum ist sie auch gefaehrlich, und deshalb steht das Einschalten hier nicht
 * allein: [initialAllowance] fuellt die Liste mit dem, was auf den Kacheln liegt. Wer die
 * Sperre einschaltet und danach vor einem Telefon steht, auf dem nichts mehr aufgeht, hat
 * keine Sperre eingerichtet, sondern sich ausgesperrt.
 */
object AppLock {

    fun needsPin(config: LauncherConfig, key: String, packageName: String): Boolean {
        if (!Pin.protects(config.security.pin, config.apps.lockOthers)) return false
        return key !in config.apps.allowed && packageName !in config.apps.allowed
    }

    /** Was das Antippen des Sperr-Schalters bewirken soll. */
    enum class Step {
        /** Aus - jede App startet ohne PIN. */
        TURN_OFF,

        /** An - es ist etwas erlaubt, das offen bleibt. */
        TURN_ON,

        /**
         * Nicht einschalten, sondern erst die Erlaubnisliste zeigen.
         *
         * Der Fall: keine App liegt auf einer Kachel, also fuellt [initialAllowance] nichts,
         * und der Schalter wuerde in einen Zustand kippen, in dem **gar nichts** mehr ohne
         * PIN aufgeht. Am Emulator mit einem Tipp erreicht - und darunter stand weiter
         * "Apps auf Deinen Kacheln sind von Anfang an erlaubt", was dort schlicht nicht
         * stimmte.
         */
        CHOOSE_FIRST,
    }

    /**
     * Wie viele Apps offen blieben, wenn die Sperre jetzt einginge.
     *
     * Die Zeile unter dem Schalter soll das sagen und nicht raten. Erst behauptete sie
     * fest, die Kachel-Apps seien erlaubt (auch bei null Kachel-Apps); danach behauptete
     * sie "keine App liegt auf einer Kachel" auch dann noch, wenn schon eine
     * Erlaubnisliste stand. Beides war eine Auskunft, die man nicht nachrechnen musste,
     * um zu merken, dass sie nicht stimmt.
     */
    fun wouldAllow(config: LauncherConfig): Int =
        if (config.apps.allowed.isNotEmpty()) {
            config.apps.allowed.size
        } else {
            initialAllowance(config).size
        }

    fun toggle(config: LauncherConfig): Step = when {
        config.apps.lockOthers -> Step.TURN_OFF
        wouldAllow(config) > 0 -> Step.TURN_ON
        else -> Step.CHOOSE_FIRST
    }

    fun toggleAllowed(apps: AppsConfig, key: String): AppsConfig =
        if (key in apps.allowed) {
            apps.copy(allowed = apps.allowed - key)
        } else {
            apps.copy(allowed = apps.allowed + key)
        }

    /**
     * Was beim Einschalten von selbst erlaubt ist: alles, was auf einer Kachel liegt.
     *
     * Diese Apps hat der Einrichtende bewusst in Reichweite gelegt; sie danach zu sperren
     * waere das Gegenteil dessen, was er gerade getan hat.
     */
    fun initialAllowance(config: LauncherConfig): Set<String> = config.screens
        .flatMap { it.cells }
        .mapNotNull { it.button.action as? ButtonAction.App }
        .map { "${it.packageName}/${it.activityName}" }
        .toSet()
}
