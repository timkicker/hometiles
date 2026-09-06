package dev.kicker.hometiles.sms.probe

import java.util.Calendar

/**
 * builds an SMS-DELIVER PDU after GSM 03.40 - a message that never came through a radio net.
 *
 * why this and not a row written straight into the database: an incoming sms starts at
 * `SmsDeliverReceiver` and goes through `Telephony.Sms.Intents.getMessagesFromIntent`. writing
 * the row skips exactly the stretch where a real message can get lost.
 *
 * debug build only: a program that can slip itself an sms does not belong in other hands.
 */
object ProbePdu {

    /**
     * digits of a number, swapped pairwise (GSM 03.40, 9.1.2.5).
     *
     * an odd count is filled up with `F` - not with `0`, which would be a digit.
     */
    fun address(number: String): ByteArray {
        val digits = number.filter { it.isDigit() }
        val filled = if (digits.length % 2 == 0) digits else digits + "F"
        val bytes = ByteArray(filled.length / 2)
        for (i in bytes.indices) {
            val left = filled[2 * i + 1].let { if (it == 'F') 0x0F else it - '0' }
            val right = filled[2 * i] - '0'
            bytes[i] = ((left shl 4) or right).toByte()
        }
        return bytes
    }

    /**
     * packs text into septets (GSM 03.38, 7 bit).
     *
     * seven bits per character, tight against each other, so the bit borders wander through
     * the bytes. check value: "hello" becomes E8 32 9B FD 06.
     */
    fun pack(text: String): ByteArray {
        val septets = text.map { it.code and 0x7F }
        val out = ArrayList<Int>()
        var carry = 0
        var bits = 0
        septets.forEach { s ->
            carry = carry or (s shl bits)
            bits += 7
            while (bits >= 8) {
                out += carry and 0xFF
                carry = carry ushr 8
                bits -= 8
            }
        }
        if (bits > 0) out += carry and 0xFF
        return out.map { it.toByte() }.toByteArray()
    }

    /** timestamp, seven bytes, every pair swapped. the last one is the time zone. */
    fun timestamp(time: Long, zoneQuarterHours: Int = 0): ByteArray {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        val parts = intArrayOf(
            calendar.get(Calendar.YEAR) % 100,
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            zoneQuarterHours,
        )
        return parts.map { value ->
            val tens = value / 10
            val ones = value % 10
            ((ones shl 4) or tens).toByte()
        }.toByteArray()
    }

    /**
     * the whole pdu.
     *
     * without the smsc part (first byte 0): the device puts that in itself. first octet 0x04 -
     * SMS-DELIVER, no further messages, no user data header.
     */
    fun build(sender: String, text: String, time: Long = System.currentTimeMillis()): ByteArray {
        val digits = sender.filter { it.isDigit() }
        val out = ArrayList<Byte>()
        out += 0x00 // no smsc
        out += 0x04 // SMS-DELIVER
        out += digits.length.toByte() // length in digits, not in bytes
        out += 0x91.toByte() // international, ISDN
        out += address(sender).toList()
        out += 0x00 // PID
        out += 0x00 // DCS: GSM 7 bit
        out += timestamp(time).toList()
        out += text.length.toByte() // length in septets
        out += pack(text).toList()
        return out.toByteArray()
    }
}
