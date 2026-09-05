package org.biglau.sms

/**
 * single messages become conversations.
 *
 * the delicate part is the grouping: the same person writes once from "+43 660 123" and once
 * from "0660123", and the provider does not always hand out the same thread id. grouping by
 * id alone shows two conversations with one person; grouping by number alone merges
 * conversations the provider keeps apart.
 *
 * we group by id and use the number only for display, which follows what one sees in every
 * other sms app.
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

    /** one line, shortened, without breaks. */
    fun preview(message: SmsMessage): String {
        val single = message.body.replace(Regex("\\s+"), " ").trim()
        return if (single.length <= PREVIEW_LENGTH) single
        else single.take(PREVIEW_LENGTH - 1).trimEnd() + "…"
    }

    /** oldest first, which is how one reads a conversation. */
    fun conversation(messages: List<SmsMessage>, threadId: Long): List<SmsMessage> =
        messages.filter { it.threadId == threadId }.sortedBy { it.timestamp }
}
