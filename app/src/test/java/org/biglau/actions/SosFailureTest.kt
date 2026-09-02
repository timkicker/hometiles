package org.biglau.actions

import org.biglau.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Warum nichts hinausging, gehört auf den Bildschirm.
 *
 * Am Emulator durchgespielt (mit entzogener SMS-Erlaubnis, es konnte also nichts hinaus):
 * nach dem Countdown stand dort **„Es konnte nichts gesendet werden."** — derselbe Satz, den
 * es auch bei einem Netzfehler gegeben hätte. Auf dem Bildschirm, der im Notfall der letzte
 * ist, ist das zu wenig: ob die Erlaubnis fehlt, niemand eingetragen ist oder das Netz nicht
 * mitspielte, sind drei verschiedene Dinge — und nur beim ersten kann der Mensch davor etwas
 * tun.
 *
 * Dazu gehört die zweite Hälfte: nach der Erlaubnis wird jetzt nur gefragt, wenn sie
 * tatsächlich fehlt. Vorher fragte der Bildschirm bei **jedem** Fehlschlag danach — und schob
 * die Schuld damit auf etwas, das gar nicht fehlte.
 */
class SosFailureTest {

    @Test
    fun `fehlende Erlaubnis bekommt ihren eigenen Satz`() {
        assertEquals(R.string.sos_failed_permission, Sos.failureText(SosFailure.NO_PERMISSION))
    }

    @Test
    fun `ohne Kontakte steht der Hinweis auf die Kontakte`() {
        assertEquals(R.string.sos_not_configured, Sos.failureText(SosFailure.NO_NUMBERS))
    }

    @Test
    fun `sonst bleibt es beim allgemeinen Satz`() {
        assertEquals(R.string.sos_failed, Sos.failureText(SosFailure.SEND_FAILED))
        assertEquals(R.string.sos_failed, Sos.failureText(SosFailure.NONE))
    }

    @Test
    fun `ein gelungener Versand hat keinen Grund zu scheitern`() {
        assertEquals(SosFailure.NONE, SosResult(sent = 2, failed = 0, hadLocation = true).failure)
    }

    @Test
    fun `ohne gesendete Nachricht ist der Grund gesetzt`() {
        val ergebnis = SosResult(sent = 0, failed = 1, hadLocation = false)
        assertTrue(ergebnis.failure != SosFailure.NONE)
    }
}
