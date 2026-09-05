package org.biglau.sms

import org.biglau.phone.CallBlocking

/**
 * hides messages from our list (`PLAN.md` 4.7).
 *
 * without the sms role we cannot reject or refuse to store anything: the message arrives
 * and stays in the system database. this only hides, and the settings texts say so.
 */
object SmsFilter {

    fun hidden(message: SmsMessage, numbers: Collection<String>, words: Collection<String>): Boolean {
        // what you wrote yourself is not hidden from you.
        if (!message.incoming) return false
        if (numbers.any { CallBlocking.key(it) == CallBlocking.key(message.address) &&
                CallBlocking.key(message.address).length >= CallBlocking.SUFFIX_DIGITS
        }) {
            return true
        }
        val text = message.body.lowercase()
        // substring, because advertising likes to append: GEWINNSPIEL!!!
        return words.any { word ->
            val wanted = word.trim().lowercase()
            wanted.isNotEmpty() && text.contains(wanted)
        }
    }

    fun apply(
        messages: List<SmsMessage>,
        numbers: Collection<String>,
        words: Collection<String>,
    ): List<SmsMessage> =
        if (numbers.isEmpty() && words.isEmpty()) messages
        else messages.filterNot { hidden(it, numbers, words) }

    fun parseWords(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

    fun formatWords(words: List<String>): String = words.joinToString(", ")
}
