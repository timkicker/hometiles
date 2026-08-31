package org.biglau.sms

/** Eine einzelne Nachricht. */
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val incoming: Boolean,
    val read: Boolean,
)

/** Ein Gespraech, wie es in der Liste steht. */
data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: SmsMessage,
    val messageCount: Int,
    val unreadCount: Int,
) {
    val hasUnread: Boolean get() = unreadCount > 0
    val title: String get() = contactName?.takeIf { it.isNotBlank() } ?: address
}
