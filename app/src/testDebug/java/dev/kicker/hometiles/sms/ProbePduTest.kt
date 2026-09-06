package dev.kicker.hometiles.sms

import dev.kicker.hometiles.sms.probe.ProbePdu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the probe pdu, byte for byte.
 *
 * a pdu that almost fits is dropped by android without a word - `getMessagesFromIntent` then
 * yields nothing, and the probe would look like a fault in HomeTiles. so the values here are
 * worked out by hand, not computed back through the same function.
 */
class ProbePduTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString(" ") { "%02X".format(it) }

    @Test
    fun `hello becomes E8 32 9B FD 06`() {
        // by hand: h=0x68 e=0x65 l=0x6C l=0x6C o=0x6F, seven bits packed tight.
        assertEquals("E8 32 9B FD 06", hex(ProbePdu.pack("hello")))
    }

    @Test
    fun `the digits are swapped pairwise`() {
        assertEquals("44 77 00 09 10 32", hex(ProbePdu.address("+447700900123")))
    }

    /** an odd digit count is filled with F - a 0 would be a digit. */
    @Test
    fun `an odd number is filled with F`() {
        assertEquals("21 43 F5", hex(ProbePdu.address("12345")))
    }

    @Test
    fun `the timestamp stands there twisted`() {
        val calendar = java.util.Calendar.getInstance().apply { set(2026, 8, 3, 22, 30, 15) }
        assertEquals("62 90 30 22 03 51 00", hex(ProbePdu.timestamp(calendar.timeInMillis)))
    }

    @Test
    fun `the head and the tail of the pdu are right`() {
        val pdu = hex(ProbePdu.build("+447700900123", "hello", 0L))
        assertTrue(
            "head is wrong: $pdu",
            pdu.startsWith("00 04 0C 91 44 77 00 09 10 32 00 00"),
        )
        assertTrue(
            "tail is wrong: $pdu",
            pdu.endsWith("05 E8 32 9B FD 06"),
        )
    }
}
