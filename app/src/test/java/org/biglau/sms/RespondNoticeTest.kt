package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * „Anruf mit Nachricht ablehnen" war ein stummer Leerlauf: der Dienst nahm die Bitte an,
 * tat nichts und hielt sich für fertig. Wer im System-Dialer „Kann jetzt nicht sprechen"
 * antippte, bekam keine Fehlermeldung — und der Anrufer bekam keine Nachricht.
 */
class RespondNoticeTest {

    @Test
    fun `der mitgegebene Text steht in der Meldung`() {
        assertEquals(
            "Bin im Zug, melde mich",
            RespondNotice.body("Bin im Zug, melde mich", "Hinweis"),
        )
    }

    @Test
    fun `ohne Text steht der Hinweis da`() {
        // Eine Meldung ohne Inhalt ist schlimmer als keine.
        assertEquals("Hinweis", RespondNotice.body("", "Hinweis"))
        assertEquals("Hinweis", RespondNotice.body("   ", "Hinweis"))
    }

    @Test
    fun `Leerraum am Rand faellt weg`() {
        assertEquals("Bin im Zug", RespondNotice.body("  Bin im Zug  ", "Hinweis"))
    }

    @Test
    fun `je Nummer eine Kennung`() {
        // Dieselbe Nummer in zwei Schreibweisen ist dieselbe Meldung - sonst stapeln sich
        // zwei Meldungen fuer denselben Anrufer.
        assertEquals(
            RespondNotice.notificationId("+436601234567"),
            RespondNotice.notificationId("0664 123 4567".replace("0664 123 4567", "+436601234567")),
        )
        assertNotEquals(
            RespondNotice.notificationId("+436601234567"),
            RespondNotice.notificationId("+436601234568"),
        )
    }

    @Test
    fun `die Kennung kollidiert nicht mit der einer Nachricht`() {
        // Sonst raeumte die eine Meldung die andere weg.
        assertNotEquals(
            SmsNotifications.notificationId("+436601234567"),
            RespondNotice.notificationId("+436601234567"),
        )
    }
}
