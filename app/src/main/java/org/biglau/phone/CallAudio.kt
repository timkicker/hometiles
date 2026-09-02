package org.biglau.phone

import org.biglau.data.AudioRoute
import org.biglau.data.PhoneConfig

/**
 * Wohin der Ton geht, wenn ein Gespräch beginnt (`PLAN.md` 4.6).
 *
 * Zwei Zusagen aus dem Plan treffen sich hier: „Standard-Audioausgabe: Hörmuschel /
 * Lautsprecher / Bluetooth" und „Lautsprecher bei abgehenden Anrufen automatisch an".
 * Der zweite ist der speziellere Fall — wer selbst wählt, hält das Telefon oft noch in der
 * Hand und schaut darauf; wer angerufen wird, hebt es ans Ohr.
 *
 * Umgestellt wird **einmal je Gespräch und nur beim Verbinden**. Bei jedem Zustandswechsel
 * nachzuziehen hieße, den Lautsprecher wieder einzuschalten, den der Nutzer gerade von Hand
 * ausgemacht hat — eine Einstellung, die die Hand des Nutzers überstimmt, ist keine
 * Einstellung, sondern ein Streit.
 */
object CallAudio {

    /**
     * Der Weg, auf den beim Verbinden gestellt werden soll — oder `null`, wenn nichts zu
     * tun ist und die Vorgabe des Systems gilt.
     */
    fun routeOnConnect(config: PhoneConfig, outgoing: Boolean): AudioRoute? = when {
        outgoing && config.speakerOnOutgoing -> AudioRoute.SPEAKER
        config.audioRoute != AudioRoute.EARPIECE -> config.audioRoute
        else -> null
    }
}
