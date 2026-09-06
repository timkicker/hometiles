package dev.kicker.hometiles.sms

/**
 * what happens to an incoming text once hometiles holds the sms role.
 *
 * **what is prevented here is loss.** android delivers `SMS_DELIVER` to the default sms app
 * only, and the role brings the duty of writing the message into the provider database
 * ourselves; otherwise nobody has it. reproduced on the emulator: took the role, sent a
 * message, and it was **nowhere** - not in the list, not in the database.
 *
 * only the arithmetic lives here; [SmsDeliverReceiver] does the writing with the android
 * classes, so the part where something can be lost stays testable.
 */
object SmsDelivery {

    /** one piece of a message as it comes off the air. */
    data class Part(val address: String, val body: String, val timestamp: Long)

    data class Incoming(val address: String, val body: String, val timestamp: Long)

    /**
     * reassembles the parts of a long text.
     *
     * past 160 characters a message arrives in pieces; storing each one gives three half
     * messages instead of one whole. the timestamp is the first part's, which is when the
     * sender wrote, not when the last piece landed.
     */
    fun merge(parts: List<Part>): List<Incoming> {
        if (parts.isEmpty()) return emptyList()
        val joined = mutableListOf<Incoming>()
        parts.forEach { part ->
            val last = joined.lastOrNull()
            if (last != null && last.address == part.address) {
                joined[joined.lastIndex] = last.copy(body = last.body + part.body)
            } else {
                joined += Incoming(part.address, part.body, part.timestamp)
            }
        }
        return joined
    }

    /** only as the default app; writing alongside it would file every message twice. */
    fun mayWrite(defaultSmsPackage: String?, ownPackage: String): Boolean =
        defaultSmsPackage != null && defaultSmsPackage == ownPackage
}
