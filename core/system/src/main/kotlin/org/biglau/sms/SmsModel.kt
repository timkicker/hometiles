package org.biglau.sms

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val incoming: Boolean,
    val read: Boolean,
    /** the network refused it; sendTextMessage throwing nothing is not a receipt. */
    val failed: Boolean = false,
)

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: SmsMessage,
    val unreadCount: Int,
) {
    val hasUnread: Boolean get() = unreadCount > 0

    /** name, else the number in the same spelling the call log uses. */
    val title: String
        get() = contactName?.takeIf { it.isNotBlank() }
            ?: org.biglau.phone.PhoneNumbers.forDisplay(address)

    /** like [title], but never blank: some carriers leave the sender empty. */
    fun titleOr(unknown: String): String = title.ifBlank { unknown }
}
