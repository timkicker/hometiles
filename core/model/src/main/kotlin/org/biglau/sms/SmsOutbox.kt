package org.biglau.sms

/**
 * columns of our own sent message.
 *
 * android files a sent text away by itself only when the sending app is *not* the default
 * one, so holding the role means writing it ourselves. whether we may write is
 * [SmsDelivery.mayWrite].
 */
object SmsOutbox {

    fun values(address: String, body: String, timestamp: Long): Map<String, Any> = mapOf(
        "address" to address,
        "body" to body,
        "date" to timestamp,
        "date_sent" to timestamp,
        // read and seen: our own message must not count as new.
        "read" to 1,
        "seen" to 1,
    )
}
