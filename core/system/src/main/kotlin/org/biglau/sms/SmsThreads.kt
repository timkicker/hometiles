package org.biglau.sms

import org.biglau.phone.PhoneNumbers

/**
 * Aus einzelnen Nachrichten werden Gespraeche.
 *
 * Der heikle Teil ist die Zuordnung: dieselbe Person schreibt mal von "+43 660 123", mal von
 * "0660123", und der Anbieter vergibt dafuer nicht immer dieselbe Gespraechskennung. Wer nur
 * nach der Kennung gruppiert, zeigt zwei Gespraeche mit derselben Person - wer nur nach der
 * Nummer gruppiert, wirft Gespraeche zusammen, die der Anbieter getrennt fuehrt.
 *
 * Wir gruppieren nach der Kennung und benutzen die Nummer nur zum Anzeigen. Das folgt dem,
 * was der Nutzer in jeder anderen SMS-App sieht.
 */
object SmsThreads {

    const val PREVIEW_LENGTH = 80

    fun group(messages: List<SmsMessage>, nameFor: (String) -> String? = { null }): List<SmsThread> =
        messages
            .groupBy { it.threadId }
            .mapNotNull { (threadId, group) ->
                val sorted = group.sortedByDescending { it.timestamp }
                val last = sorted.firstOrNull() ?: return@mapNotNull null
                SmsThread(
                    threadId = threadId,
                    address = last.address,
                    contactName = nameFor(last.address),
                    lastMessage = last,
                    unreadCount = sorted.count { it.incoming && !it.read },
                )
            }
            .sortedByDescending { it.lastMessage.timestamp }

    /** Vorschautext fuer die Liste: eine Zeile, gekuerzt, ohne Umbrueche. */
    fun preview(message: SmsMessage): String {
        val single = message.body.replace(Regex("\\s+"), " ").trim()
        return if (single.length <= PREVIEW_LENGTH) single
        else single.take(PREVIEW_LENGTH - 1).trimEnd() + "…"
    }

    /** Nachrichten eines Gespraechs, aelteste zuerst - so liest man eine Unterhaltung. */
    fun conversation(messages: List<SmsMessage>, threadId: Long): List<SmsMessage> =
        messages.filter { it.threadId == threadId }.sortedBy { it.timestamp }
}
