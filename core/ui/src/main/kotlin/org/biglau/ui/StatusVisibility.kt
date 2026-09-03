package org.biglau.ui

import org.biglau.data.Appearance
import org.biglau.data.ClockDisplay

/**
 * Sieht der Nutzer noch, wie spaet es ist und wie voll der Akku? PLAN.md 4.2 verlangt eine
 * „eigene grosse Batterie- und Signalanzeige (fuers Vollbild)" - der Grund dafuer ist
 * genau diese Frage.
 *
 * Im Vollbild ist die Statusleiste des Systems weg. Die Kopfzeile der App traegt beides,
 * solange sie an ist. Beides zugleich auszuschalten ist erlaubt - es ist ein Startbildschirm
 * und kein Cockpit -, aber es ist die Sorte Einstellung, die man versehentlich trifft und
 * dann nicht mehr zuordnet: das Telefon zeigt einfach keine Uhrzeit mehr, und man sucht
 * den Fehler beim Telefon.
 */
object StatusVisibility {

    /** Zeigt irgendetwas die Uhrzeit? */
    fun showsTime(appearance: Appearance): Boolean = when {
        !appearance.fullScreen -> true
        !appearance.showHeader -> false
        else -> appearance.clock != ClockDisplay.OFF
    }

    /** Zeigt irgendetwas den Ladestand? */
    fun showsBattery(appearance: Appearance): Boolean =
        !appearance.fullScreen || appearance.showHeader

    /** Fehlt eines von beiden, gehoert ein Hinweis daneben - nicht ein Verbot. */
    fun warns(appearance: Appearance): Boolean =
        !showsTime(appearance) || !showsBattery(appearance)
}
