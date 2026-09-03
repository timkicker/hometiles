package org.biglau.sms

import org.biglau.sms.probe.ProbePdu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das Probe-PDU, Byte fuer Byte.
 *
 * Ein PDU, das fast stimmt, wird von Android wortlos verworfen - `getMessagesFromIntent`
 * liefert dann nichts, und die Probe saehe aus wie ein Fehler in BigLau. Deshalb stehen
 * hier von Hand ausgerechnete Werte und keine Rueckrechnung durch dieselbe Funktion.
 */
class ProbePduTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString(" ") { "%02X".format(it) }

    @Test
    fun `hello wird zu E8 32 9B FD 06`() {
        // Von Hand: h=0x68 e=0x65 l=0x6C l=0x6C o=0x6F, sieben Bit dicht gepackt.
        assertEquals("E8 32 9B FD 06", hex(ProbePdu.packen("hello")))
    }

    @Test
    fun `die Ziffern werden paarweise vertauscht`() {
        assertEquals("44 77 00 09 10 32", hex(ProbePdu.adresse("+447700900123")))
    }

    /** Ungerade Ziffernzahl wird mit F aufgefuellt - 0 waere eine Ziffer. */
    @Test
    fun `eine ungerade Nummer wird mit F aufgefuellt`() {
        assertEquals("21 43 F5", hex(ProbePdu.adresse("12345")))
    }

    @Test
    fun `der Zeitstempel steht verdreht da`() {
        val kalender = java.util.Calendar.getInstance().apply { set(2026, 8, 3, 22, 30, 15) }
        assertEquals("62 90 30 22 03 51 00", hex(ProbePdu.zeitstempel(kalender.timeInMillis)))
    }

    @Test
    fun `der Kopf und das Ende des PDU stimmen`() {
        val pdu = hex(ProbePdu.baue("+447700900123", "hello", 0L))
        assertTrue(
            "Kopf stimmt nicht: $pdu",
            pdu.startsWith("00 04 0C 91 44 77 00 09 10 32 00 00"),
        )
        assertTrue(
            "Ende stimmt nicht: $pdu",
            pdu.endsWith("05 E8 32 9B FD 06"),
        )
    }
}
