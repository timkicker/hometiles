package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Bildnachricht, die nicht ankommt, sagt Bescheid.
 *
 * `PLAN.md` 6 verlangt es für MMS ausdrücklich: „bei Fehlschlag **sichtbar an den Nutzer
 * melden statt still schlucken". Der Empfänger tat bis zum 3.9.2026 genau das Gegenteil: er
 * merkte sich, dass sich etwas geändert hat, und schwieg. Wer ein Bild erwartet, wartet auf
 * etwas, das nie kommt — und hält das Telefon für kaputt, nicht die App für ehrlich.
 *
 * Der Hinweis nennt beides: den Grund (kein Netzzugang) und den Ausweg (die Nachrichten-App
 * des Telefons). Eine Meldung, die ein Problem nennt, ohne einen Weg zu zeigen, ist nur die
 * halbe Antwort — dieselbe Regel wie beim Notruf-Knopf und bei der toten Kachel.
 */
class MmsHinweisTest {

    private val empfaenger = Quelltext.datei("org/biglau/sms/SmsComponents.kt").readText()
    private val meldungen = Quelltext.datei("org/biglau/sms/SmsNotifications.kt").readText()

    @Test
    fun `der WAP-Push-Empfaenger schweigt nicht mehr`() {
        val stelle = empfaenger.substringAfter("class WapPushDeliverReceiver")
            .substringBefore("\n}")
        assertTrue(
            "WapPushDeliverReceiver meldet die Bildnachricht nicht mehr - dann kommt sie " +
                "still an und niemand erfährt davon.",
            "showMmsHint(" in stelle,
        )
    }

    /**
     * Der Kanal darf **nicht** mit dem Präfix der SMS-Kanäle anfangen.
     *
     * `show` räumt beim Anlegen seines Kanals alle anderen mit demselben Präfix weg — das
     * ist Absicht, damit nicht für jede Vibrationsdauer ein alter Kanal liegenbleibt. Hiesse
     * der MMS-Kanal `sms-…`, verschwände der Hinweis beim nächsten SMS-Eingang mitsamt
     * seinem Kanal.
     */
    @Test
    fun `der MMS-Kanal faellt nicht der Aufraeumschleife zum Opfer`() {
        // Gefragt ist die Kennung, nicht wie die Konstante heisst. Die erste Fassung suchte
        // `MMS_CHANNEL = "…"`; am 03.09.2026 wurde daraus `MMS_CHANNEL_PREFIX`, weil die
        // Vibrationsstaerke in die Kennung musste - und die Regel fiel um, obwohl die Sache
        // dieselbe geblieben war.
        val kanal = Regex("""MMS_CHANNEL(?:_PREFIX)? = "([^"]+)"""")
            .find(meldungen)?.groupValues?.get(1)
        assertTrue("Den MMS-Kanal gibt es nicht mehr", kanal != null)
        val praefix = Regex("""[^_]CHANNEL_PREFIX = "([^"]+)"""").find(meldungen)!!.groupValues[1]
        assertEquals(
            "Der MMS-Kanal beginnt mit dem Präfix der SMS-Kanäle und wird beim nächsten " +
                "SMS-Hinweis weggeräumt.",
            false,
            kanal!!.startsWith(praefix),
        )
    }

    @Test
    fun `der Hinweis nennt Grund und Ausweg, in beiden Sprachen`() {
        listOf("values" to listOf("network", "messaging app"),
               "values-de" to listOf("Netzzugang", "Nachrichten-App"))
            .forEach { (sprache, woerter) ->
                val text = Regex("""<string name="mms_arrived_body">(.*?)</string>""")
                    .find(Quelltext.texte(sprache).first().readText())
                    ?.groupValues?.get(1)
                    ?: throw AssertionError("$sprache: mms_arrived_body fehlt")
                woerter.forEach {
                    assertTrue("$sprache: der Hinweis sagt nichts über „$it\"", it in text)
                }
            }
    }
}
