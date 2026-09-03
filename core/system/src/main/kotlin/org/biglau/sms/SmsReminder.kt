package org.biglau.sms

import org.biglau.data.SmsConfig
import org.biglau.phone.PhoneNumbers

/**
 * Die wiederholte Erinnerung an ungelesene Nachrichten. `PLAN.md` 4.7.
 *
 * Wofür sie da ist: eine Meldung, die einmal kommt, verpasst man. Wer das Telefon in der
 * Tasche hat und die Vibration nicht spürt, sieht die Nachricht erst am Abend. Das Original
 * kann das, und es ist für den gedachten Nutzer eine der nützlicheren Einstellungen.
 *
 * Erinnert wird nur an **ungelesene eingehende** Nachrichten, je Absender an die neueste.
 * Damit hört die Kette von selbst auf, sobald jemand die Unterhaltung öffnet - eine
 * Erinnerung, die man nur durch Ausschalten loswird, wäre schlimmer als keine.
 */
object SmsReminder {

    /** Abstände zur Wahl, in Minuten. Null heisst: nicht erinnern. */
    val CHOICES = listOf(0, 2, 5, 15)

    fun active(config: SmsConfig): Boolean = config.repeatMinutes > 0

    /** Der Abstand in Millisekunden. Eine Stelle dafuer, damit Wecker und Anzeige nicht auseinanderlaufen. */
    fun delayMs(minutes: Int): Long = minutes.toLong() * 60_000L

    /**
     * Höchstens so viele Meldungen auf einmal.
     *
     * Am Emulator ausprobiert und prompt hineingelaufen: die erste Erinnerung brachte
     * **zwölf** Meldungen auf einmal - für jede ungelesene Unterhaltung eine. Genau das
     * passiert auch am echten Gerät, sobald jemand BigLau zur Standard-App macht und ein
     * Rückstand ungelesener Nachrichten da liegt. Mehr als drei Meldungen auf einmal sind
     * kein Hinweis mehr, sondern eine Wand.
     */
    const val MAX_AT_ONCE = 3

    /**
     * Woran erinnert wird: je Absender die neueste ungelesene Nachricht, neueste zuerst,
     * höchstens [MAX_AT_ONCE].
     *
     * Was ausgeblendet ist, erinnert auch nicht - sonst käme die Werbenachricht, die man
     * nicht sehen wollte, alle fünf Minuten wieder.
     */
    fun due(messages: List<SmsMessage>, config: SmsConfig): List<SmsMessage> =
        messages
            .filter { it.incoming && !it.read }
            .filterNot { SmsFilter.hidden(it, config.hiddenNumbers, config.hiddenWords) }
            .groupBy { PhoneNumbers.clean(it.address).ifEmpty { it.address } }
            .mapNotNull { (_, gruppe) -> gruppe.maxByOrNull { it.timestamp } }
            .sortedByDescending { it.timestamp }
            .take(MAX_AT_ONCE)
}
