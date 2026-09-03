package org.biglau.sms

import org.biglau.phone.CallBlocking

/**
 * Was in der Nachrichtenliste nicht erscheinen soll (`PLAN.md` 4.7).
 *
 * **Was das ist und was nicht:** BigLau hält die SMS-Rolle nicht, kann eine Nachricht also
 * weder abweisen noch am Speichern hindern — sie kommt an und liegt in der Datenbank des
 * Systems. Was hier passiert, ist deshalb ein *Verbergen*: die gefilterte Nachricht steht
 * nicht in unserer Liste. Das ist weniger, als „filtern" verspricht, und wird auch so
 * benannt (siehe die Texte in den Einstellungen) — eine Sperre, die man für dichter hält,
 * als sie ist, ist gefährlicher als eine, deren Grenze man kennt.
 *
 * Nummern werden wie bei der Anrufsperre über die letzten Ziffern verglichen; Wörter ohne
 * Rücksicht auf Groß- und Kleinschreibung und als Teilzeichenkette, weil Werbung ihre
 * Wörter gern anhängt („GEWINNSPIEL!!!").
 */
object SmsFilter {

    fun hidden(message: SmsMessage, numbers: Collection<String>, words: Collection<String>): Boolean {
        // Nur Eingehendes: was man selbst geschrieben hat, verbirgt man nicht vor sich.
        if (!message.incoming) return false
        if (numbers.any { CallBlocking.key(it) == CallBlocking.key(message.address) &&
                CallBlocking.key(message.address).length >= CallBlocking.SUFFIX_DIGITS
        }) {
            return true
        }
        val text = message.body.lowercase()
        return words.any { wort ->
            val gesucht = wort.trim().lowercase()
            gesucht.isNotEmpty() && text.contains(gesucht)
        }
    }

    fun apply(
        messages: List<SmsMessage>,
        numbers: Collection<String>,
        words: Collection<String>,
    ): List<SmsMessage> =
        if (numbers.isEmpty() && words.isEmpty()) messages
        else messages.filterNot { hidden(it, numbers, words) }

    /** Zerlegt die Wortliste. Leere Einträge fallen weg, Dubletten auch. */
    fun parseWords(text: String): List<String> = text
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

    fun formatWords(words: List<String>): String = words.joinToString(", ")
}
