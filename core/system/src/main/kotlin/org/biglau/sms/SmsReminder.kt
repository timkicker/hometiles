package org.biglau.sms

import org.biglau.data.SmsConfig
import org.biglau.phone.PhoneNumbers

/**
 * repeated reminder about unread messages. `PLAN.md` 4.7.
 *
 * only unread incoming ones, newest per sender: the chain then stops by itself once the
 * conversation is opened.
 */
object SmsReminder {

    /** intervals to pick from, in minutes. zero means no reminder. */
    val CHOICES = listOf(0, 2, 5, 15)

    fun active(config: SmsConfig): Boolean = config.repeatMinutes > 0

    fun delayMs(minutes: Int): Long = minutes.toLong() * 60_000L

    /** measured at the emulator: a backlog produced twelve notices at once. */
    const val MAX_AT_ONCE = 3

    /** hidden messages do not remind either. */
    fun due(messages: List<SmsMessage>, config: SmsConfig): List<SmsMessage> =
        messages
            .filter { it.incoming && !it.read }
            .filterNot { SmsFilter.hidden(it, config.hiddenNumbers, config.hiddenWords) }
            .groupBy { PhoneNumbers.clean(it.address).ifEmpty { it.address } }
            .mapNotNull { (_, group) -> group.maxByOrNull { it.timestamp } }
            .sortedByDescending { it.timestamp }
            .take(MAX_AT_ONCE)
}
