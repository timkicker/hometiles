package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Gesendet" muss eine Tatsache sein, keine Hoffnung.
 *
 * `SmsManager.sendTextMessage` kehrt sofort zurueck. Ob das Netz die Nachricht ueberhaupt
 * genommen hat, steht erst Sekunden spaeter fest und kommt als Rundruf - **wenn** man einen
 * `PendingIntent` mitgibt. Bis zum 03.09.2026 stand dort `null`: BigLau meldete „gesendet",
 * weil der Aufruf keine Ausnahme geworfen hatte, schrieb die Nachricht in den Ausgang, und
 * eine vom Netz abgelehnte Nachricht sah danach aus wie jede andere.
 *
 * Fuer ein Telefon, auf das sich jemand verlaesst, ist das der schlimmere der beiden Faelle:
 * nicht senden zu koennen ist ein Problem, aber zu glauben, man haette gesendet, ist ein
 * Problem, von dem man nichts weiss. `PLAN.md` P6 nennt Zustellberichte seit jeher; der
 * Quelltext hatte nicht einmal die Sendequittung.
 */
class QuittungTest {

    private val senden = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `keine Nachricht geht ohne Quittung hinaus`() {
        val ohne = senden.lines().withIndex()
            .filter { (_, z) -> "sendTextMessage(" in z || "sendMultipartTextMessage(" in z }
            .filter { (i, _) ->
                // Der Aufruf steht ueber mehrere Zeilen; die Quittung darf in den
                // naechsten acht stehen.
                senden.lines().drop(i).take(8).none { "receipt" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "Hier geht eine Nachricht ohne Quittung hinaus. Dann heisst gesendet nur, " +
                "dass der Aufruf nicht geworfen hat - und eine abgelehnte Nachricht sieht " +
                "aus wie eine angekommene.",
            emptyList<Int>(),
            ohne,
        )
    }

    @Test
    fun `die Zeile steht schon da, bevor gesendet wird`() {
        val beimSchreiben = senden.indexOf("Telephony.Sms.Sent.CONTENT_URI")
        val beimSenden = senden.indexOf("sendTextMessage(")
        assertTrue("Der Ausgang wird nicht mehr geschrieben", beimSchreiben > 0)
        assertTrue(
            "Geschrieben wird erst nach dem Senden. Dann kann die Quittung nicht sagen, " +
                "**welche** Nachricht nicht durchkam.",
            beimSchreiben < beimSenden,
        )
    }

    @Test
    fun `die Quittung macht aus dem Fehlschlag etwas Sichtbares`() {
        val empfaenger = Quelltext.withoutComments("org/biglau/sms/SmsSentReceiver.kt")
        assertTrue(
            "Der Fehlschlag wird nicht in der Datenbank vermerkt - dann steht die " +
                "Nachricht weiter da, als waere sie heraus.",
            "MESSAGE_TYPE_FAILED" in empfaenger,
        )
        assertTrue(
            "Niemand erfaehrt davon. Wer auf Senden getippt hat, hat den Bildschirm " +
                "meist schon verlassen - eine Meldung im Gespraech waere nur zu sehen, " +
                "wenn man ohnehin hinschaut.",
            "showSendFailed" in empfaenger,
        )
        assertTrue(
            "Der Grund wird nicht genannt. Ein allgemeiner Fehler ist keine Auskunft; " +
                "kein Empfang ist eine.",
            "RESULT_ERROR_NO_SERVICE" in empfaenger,
        )
    }

    @Test
    fun `eine nicht gesendete Nachricht sieht anders aus`() {
        assertTrue(
            "Im Gespraech ist einer nicht gesendeten Nachricht nichts anzusehen. Sie steht " +
                "in derselben Reihe an derselben Stelle - der einzige Unterschied waere, " +
                "dass keine Antwort kommt.",
            "message.failed" in senden && "R.string.sms_not_sent" in senden,
        )
    }
}
