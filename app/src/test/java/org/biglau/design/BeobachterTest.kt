package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Liste folgt der Datenbank, nicht nur sich selbst.
 *
 * `SmsRepository.changes` tickte nur, wenn BigLau schrieb - eine neue Nachricht, ein
 * Als-gelesen-Markieren. Aenderte jemand anderes etwas, stand die Liste still. Am
 * 03.09.2026 genau so gesehen: zwei Nachrichten waren aus der Datenbank geloescht und
 * standen weiter da, bis die App neu startete.
 *
 * Das ist derselbe Fehler wie bei den Zustaenden, die das System vergibt (siehe
 * `SystemzustandTest`): einmal gelesen, nie wieder gefragt. Nur ist die Quelle hier keine
 * Rolle, sondern eine Datenbank - und die sagt von sich aus Bescheid, wenn man sie fragt.
 */
class BeobachterTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `der Nachrichtenbildschirm horcht auf die Datenbank`() {
        assertTrue(
            "SmsActivity meldet keinen Beobachter mehr an. Dann zeigt die Liste weiter, " +
                "was laengst geloescht ist - und verschweigt, was von woanders dazukam.",
            "registerContentObserver" in quelle && "Telephony.Sms.CONTENT_URI" in quelle,
        )
    }

    @Test
    fun `und meldet ihn wieder ab`() {
        assertTrue(
            "Der Beobachter wird nicht abgemeldet - er ueberlebt den Bildschirm und haelt " +
                "ihn am Leben.",
            "unregisterContentObserver" in quelle,
        )
        val anmelden = quelle.indexOf("registerContentObserver")
        val abmelden = quelle.indexOf("unregisterContentObserver")
        assertTrue(
            "Abmelden steht nicht hinter dem Anmelden - dann gehoert es nicht zu ihm.",
            abmelden > anmelden,
        )
    }

    @Test
    fun `der Beobachter meldet die Aenderung weiter`() {
        val ab = quelle.indexOf("ContentObserver(")
        assertTrue("Es gibt keinen Beobachter mehr", ab > 0)
        val rumpf = quelle.substring(ab, minOf(quelle.length, ab + 400))
        assertTrue(
            "Der Beobachter tut nichts mit dem, was er hoert.",
            "notifyChanged" in rumpf,
        )
    }
}
