package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was hinausgeht, geht nur auf eine Handbewegung hinaus.
 *
 * BigLau kann anrufen und Nachrichten senden — es ist ein Telefon. Die Gefahr ist nicht die
 * Fähigkeit, sondern **wo** sie steht: ein Empfänger, ein Dienst oder der Programmstart
 * haben keinen Menschen davor. Was von dort aus wählt, wählt auch nachts um vier.
 *
 * Die Sperre in `STATUS.md` ist eine Vereinbarung mit dem Nutzer, kein Schloss: die
 * Berechtigungen `CALL_PHONE` und `SEND_SMS` sind auf seinem Telefon **erteilt**. Bis zum
 * 3.9.2026 hat diese Vereinbarung an keiner Stelle im Quelltext gestanden. Jetzt steht sie
 * hier.
 *
 * Zwei Zusicherungen:
 *
 * 1. Nur die aufgezählten Dateien dürfen wählen oder senden. Eine neue fällt hier auf,
 *    bevor sie jemandem auffällt, der ungewollt angerufen wird.
 * 2. Keine dieser Dateien ist ein `BroadcastReceiver`, ein `Service` oder die
 *    `Application` — dort ist niemand, der die Bewegung gemacht hätte.
 */
class HinausTest {

    /** Datei → wer die Handbewegung macht. */
    private val darfHinaus = mapOf(
        "DialerActivity.kt" to "der Anrufknopf auf der Wähltastatur, nach einem Tipp",
        "Intents.kt" to "baut die Absicht; ausgelöst wird sie von einem Bildschirm",
        "Sos.kt" to "der Notruf, nach Countdown und mit Abbruchknopf (SosActivity)",
        "SmsActivity.kt" to "der Sendeknopf im Gespräch, nach einem Tipp",
    )

    private val hinausMuster = Regex("""ACTION_CALL|sendTextMessage|sendMultipartTextMessage""")

    private fun stellen(): Map<File, List<String>> = Quelltext.dateien()
        .associateWith { datei ->
            datei.readLines().filter { zeile ->
                hinausMuster.containsMatchIn(zeile) &&
                    !zeile.trim().startsWith("*") &&
                    !zeile.trim().startsWith("//")
            }
        }
        .filterValues { it.isNotEmpty() }

    @Test
    fun `nur benannte stellen duerfen waehlen oder senden`() {
        assertEquals(
            "Eine neue Stelle wählt oder sendet. Das ist die eine Sache, die in dieser App " +
                "nie versehentlich passieren darf - mit Grund in die Liste in HinausTest, " +
                "oder weg damit.",
            darfHinaus.keys.sorted(),
            stellen().keys.map { it.name }.sorted(),
        )
    }

    @Test
    fun `keine dieser stellen laeuft ohne einen menschen davor`() {
        val ohneMenschen = stellen().keys.filter { datei ->
            val text = datei.readText()
            Regex(""": *(BroadcastReceiver|Service|Application)\b""").containsMatchIn(text)
        }.map { it.name }
        assertEquals(
            "Hier wird gewählt oder gesendet, wo niemand davorsteht - ein Empfänger, ein " +
                "Dienst oder der Programmstart. Genau das darf nicht sein.",
            emptyList<String>(),
            ohneMenschen,
        )
    }

    /** Datei → wer die Handbewegung macht, für die Stellen, die über `Intents` gehen. */
    private val darfUeberIntentsHinaus = mapOf(
        "ContactsActivity.kt" to "ein Tipp auf einen Kontakt in der Liste",
        "MainActivity.kt" to "eine Kontaktkachel, und der PIN-Ablauf davor",
    )

    /**
     * Auch die **Auftraggeber** zählen.
     *
     * `Intents.call` und `Intents.sms` bauen die Absicht; wer sie ruft, löst sie aus. Die
     * Liste oben sieht solche Aufrufer nicht, denn in ihnen steht kein `ACTION_CALL` — am
     * 3.9.2026 waren das drei Stellen in zwei Dateien, alle in Ordnung, aber ungeprüft.
     *
     * Ein neuer Aufruf in einem Empfänger oder Dienst wäre genau das, was diese Regel
     * verhindern soll: wählen, wo niemand davorsteht.
     */
    @Test
    fun `auch wer ueber Intents waehlt oder schreibt, steht in der Liste`() {
        val muster = Regex("""Intents\.(call|sms)\(""")
        val stellen = Quelltext.dateien()
            .filter { datei ->
                datei.name != "Intents.kt" &&
                    datei.readLines().any { muster.containsMatchIn(it) && !it.trim().startsWith("//") }
            }
        assertEquals(
            "Eine neue Stelle löst einen Anruf oder eine Nachricht aus. Mit Grund in die " +
                "Liste in HinausTest, oder weg damit.",
            darfUeberIntentsHinaus.keys.sorted(),
            stellen.map { it.name }.sorted(),
        )
        val ohneMenschen = stellen.filter {
            Regex(""": *(BroadcastReceiver|Service|Application)\b""").containsMatchIn(it.readText())
        }.map { it.name }
        assertEquals(
            "Hier wird gewählt oder geschrieben, wo niemand davorsteht",
            emptyList<String>(),
            ohneMenschen,
        )
    }

    @Test
    fun `jede ausnahme nennt ihre handbewegung`() {
        (darfHinaus + darfUeberIntentsHinaus).forEach { (datei, grund) ->
            assertTrue("$datei: Grund fehlt oder ist zu knapp", grund.length > 20)
        }
    }
}
