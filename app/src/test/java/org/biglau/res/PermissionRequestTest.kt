package org.biglau.res

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer ein gefährliches Recht im Manifest anmeldet, muss es auch erfragen.
 *
 * Anlass: `WRITE_CALL_LOG` stand im Manifest, wurde vor jedem Löschen geprüft — und von
 * keiner Zeile je erfragt. Am Telefon des Nutzers stand es auf `granted=false`. Wer dort
 * einen Anruf löschte, bestätigte die Rückfrage und sah die Zeile unverändert stehen: eine
 * Sackgasse, aus der nichts herausführte, weil nichts fragte. Dieselbe Lücke hatte der
 * Notruf beim Standort — die Nachricht versprach Koordinaten und ging ohne hinaus.
 *
 * Ein angemeldetes Recht ohne Frage ist immer eine dieser beiden Sackgassen. Angemeldet
 * wird es ja, weil eine Stelle im Code es braucht.
 */
class PermissionRequestTest {

    private val manifest = File("src/main/AndroidManifest.xml")
    private val quelltext = File("src/main/java")

    /** Nur diese Gruppe fragt Android zur Laufzeit ab; der Rest wird beim Installieren erteilt. */
    private val gefaehrlich = setOf(
        "CALL_PHONE", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_PHONE_STATE",
        "READ_CONTACTS", "WRITE_CONTACTS", "SEND_SMS", "READ_SMS", "RECEIVE_SMS",
        "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
    )

    /**
     * Rechte, die nicht erfragt werden können, weil sie an einer Rolle hängen.
     *
     * `RECEIVE_SMS` erteilt Android der App, die die SMS-Rolle hält, und nur ihr — ein
     * eigener Dialog dafür existiert nicht. Wer die Rolle nicht hält, bekommt das Recht
     * auch nach jeder Frage nicht.
     */
    private val ueberEineRolle = setOf("RECEIVE_SMS")

    private fun angemeldet(): Set<String> =
        Regex("""uses-permission android:name="android\.permission\.([A-Z_]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it in gefaehrlich }
            .toSet()

    /** Alles, was irgendwo in einem `launch(…)` steht — einzeln oder im `arrayOf(…)`. */
    private fun erfragt(): Set<String> {
        val treffer = mutableSetOf<String>()
        quelltext.walkTopDown().filter { it.extension == "kt" }.forEach { datei ->
            val text = datei.readText()
            Regex("""\.launch\(""").findAll(text).forEach { start ->
                // Bis zur schliessenden Klammer des launch-Aufrufs lesen, damit ein
                // arrayOf(…) ueber mehrere Zeilen mitgenommen wird.
                var tiefe = 1
                var i = start.range.last + 1
                while (i < text.length && tiefe > 0) {
                    when (text[i]) {
                        '(' -> tiefe++
                        ')' -> tiefe--
                    }
                    i++
                }
                Regex("""Manifest\.permission\.([A-Z_]+)""")
                    .findAll(text.substring(start.range.last + 1, i))
                    .forEach { treffer += it.groupValues[1] }
            }
        }
        return treffer
    }

    @Test
    fun `jedes gefaehrliche Recht wird auch erfragt`() {
        val fehlt = (angemeldet() - erfragt() - ueberEineRolle).sorted()
        assertEquals(
            "Diese Rechte stehen im Manifest, werden aber nie erfragt. Wer sie braucht, " +
                "steht vor einer Sackgasse: $fehlt",
            emptyList<String>(),
            fehlt,
        )
    }

    @Test
    fun `die Regel liest auch Rechte aus einem arrayOf`() {
        // Gegenprobe: der Assistent fragt READ_CONTACTS und CALL_PHONE nur im Rudel ab.
        // Ohne das Klammerzaehlen faende die Regel beides nicht.
        assertTrue("CALL_PHONE steht in einem arrayOf", "CALL_PHONE" in erfragt())
    }

    @Test
    fun `die Regel erfindet keine Treffer`() {
        assertTrue("READ_CALENDAR wird nirgends erfragt", "READ_CALENDAR" !in erfragt())
    }
}
