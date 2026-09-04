package org.biglau.sms

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die vier Pflichtkomponenten der Standard-SMS-Rolle stehen im Manifest.
 *
 * Android verlangt von einer Standard-SMS-App vier Dinge, und zwar alle: einen Empfänger für
 * `SMS_DELIVER`, einen für `WAP_PUSH_DELIVER` (MMS), einen Dienst für „mit Nachricht
 * antworten" und eine Activity für `SENDTO`. Fehlt **eine** davon, erscheint BigLau in der
 * Auswahl der Standard-SMS-App gar nicht erst — ohne Fehler, ohne Meldung, ohne dass
 * irgendein Test rot wird.
 *
 * Der README verspricht „alle vier Pflichtkomponenten sind da", und `SmsComponents.kt` sagt
 * es im Kommentar. Geprüft hat es bis zum 3.9.2026 nichts: die Tests daneben prüfen, was die
 * Empfänger *tun*, nicht dass es sie gibt.
 */
class SmsRolleTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun enthaelt(vararg teile: String) = teile.all { it in manifest }

    @Test
    fun `der Empfaenger fuer eingehende Nachrichten ist da`() {
        assertTrue(
            "SMS_DELIVER fehlt - BigLau bekommt eingehende Nachrichten dann nie zu sehen",
            enthaelt(
                "android.provider.Telephony.SMS_DELIVER",
                "android.permission.BROADCAST_SMS",
            ),
        )
    }

    @Test
    fun `der Empfaenger fuer MMS ist da`() {
        assertTrue(
            "WAP_PUSH_DELIVER oder sein MIME-Typ fehlt",
            enthaelt(
                "android.provider.Telephony.WAP_PUSH_DELIVER",
                "application/vnd.wap.mms-message",
                "android.permission.BROADCAST_WAP_PUSH",
            ),
        )
    }

    @Test
    fun `der Dienst zum Antworten waehrend eines Anrufs ist da`() {
        assertTrue(
            "RESPOND_VIA_MESSAGE fehlt - ohne ihn steht BigLau nicht in der Auswahl",
            enthaelt(
                "android.intent.action.RESPOND_VIA_MESSAGE",
                "android.permission.SEND_RESPOND_VIA_MESSAGE",
            ),
        )
    }

    @Test
    fun `die Activity fuer SENDTO kennt alle vier Schemata`() {
        // Ab dem Namensattribut: ein Kommentar, der den Namen erwähnt, würde den Schnitt
        // sonst zu früh setzen - im Nachbartest ist genau das passiert.
        val block = Quelltext.ausschnitt(manifest, "\".sms.SmsActivity\"", "</activity>")
        listOf("\"sms\"", "\"smsto\"", "\"mms\"", "\"mmsto\"").forEach { schema ->
            assertTrue("SENDTO ohne Schema $schema", schema in block)
        }
        assertTrue("SENDTO fehlt", "android.intent.action.SENDTO" in block)
    }
}
