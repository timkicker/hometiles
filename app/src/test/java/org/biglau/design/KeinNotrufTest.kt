package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Notruf ruft niemanden an.
 *
 * BigLaus SOS verschickt Nachrichten, mehr nicht - kein Anruf, erst recht keiner an 112.
 * Das war immer so und stand nirgends als Regel; es stand nur da, weil es niemand
 * hingeschrieben hatte. `PLAN.md` 4.8 und P7 nennen dagegen einen „automatischen Anruf",
 * und ein Plan, der mehr verspricht als der Quelltext tut, ist der Anfang davon, dass es
 * jemand einbaut.
 *
 * Die Regel steht hier, weil der Preis eines Fehlers hier nicht in Bildschirmen gemessen
 * wird: ein versehentlicher Notruf bindet Menschen, die gerade woanders gebraucht werden.
 * Die Notrufnummern in [org.biglau.phone.PhoneNumbers] gibt es fuer das Gegenteil - die
 * Waehltastatur soll sie **erkennen** und an den System-Dialer abgeben, statt selbst zu
 * waehlen.
 */
class KeinNotrufTest {

    /** Alles, was am Notruf beteiligt ist. */
    private val notrufDateien = Quelltext.files()
        .filter { it.name.startsWith("Sos") }

    @Test
    fun `kein Teil des Notrufs waehlt`() {
        assertTrue(
            "Keine einzige Sos-Datei gefunden - liest die Regel noch, was sie meint?",
            notrufDateien.size >= 5,
        )
        val waehlt = notrufDateien.flatMap { datei ->
            datei.readLines().withIndex()
                .filter { (_, z) ->
                    val nackt = z.trim()
                    !Quelltext.isCommentLine(z) &&
                        ("ACTION_CALL" in nackt || "Intents.call(" in nackt)
                }
                .map { (i, z) -> "${datei.name}:${i + 1}: ${z.trim()}" }
        }
        assertEquals(
            "Hier waehlt der Notruf. BigLaus SOS verschickt Nachrichten - ein Anruf von " +
                "hier aus kann eine Notrufnummer treffen, und den Fehler zahlt jemand " +
                "anderes.",
            emptyList<String>(),
            waehlt,
        )
    }

    @Test
    fun `keine Notrufnummer steht im Notruf-Weg`() {
        val genannt = notrufDateien.flatMap { datei ->
            datei.readLines().withIndex()
                .filter { (_, z) -> "WELL_KNOWN_EMERGENCY" in z || "looksLikeEmergency" in z }
                .map { (i, _) -> "${datei.name}:${i + 1}" }
        }
        assertEquals(
            "Der Notruf-Weg fasst die Notrufnummern an. Die Liste gibt es, damit die " +
                "Waehltastatur sie erkennt und abgibt - nicht, damit irgendwer sie waehlt.",
            emptyList<String>(),
            genannt,
        )
    }

    @Test
    fun `die Probe erreicht das Senden nicht`() {
        // Ohne Kommentare: der Zweig traegt selbst den Satz „Kein Sos.send", und die
        // erste Fassung dieser Regel las ihn als Aufruf. Ein Kommentar, der den Namen
        // nur nennt, ist kein Aufruf.
        val quelle = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")
        val ab = quelle.indexOf("if (preview) {")
        assertTrue("Den Probe-Zweig gibt es nicht mehr", ab > 0)
        val zweig = quelle.substring(ab, quelle.indexOf("return@LaunchedEffect", ab))
        assertTrue(
            "Die Probe kommt bis zum Senden. Eine Probe, die sendet, ist keine.",
            "Sos.send" !in zweig,
        )
        assertTrue(
            "Die Probe zeigt nicht mehr, was hinausginge - dann liesse sich der Text nur " +
                "herausfinden, indem man ihn abschickt.",
            "Sos.compose" in zweig,
        )
    }

    @Test
    fun `ohne Nummer geht nichts hinaus`() {
        val send = Quelltext.file("org/biglau/toggles/Sos.kt").readText()
            .let { Quelltext.cut(it, "fun send(") }
        val erste = send.lines().drop(1).first { it.isNotBlank() }
        assertTrue(
            "Die erste Zeile von Sos.send prueft nicht mehr, ob ueberhaupt eine Nummer " +
                "eingetragen ist: $erste",
            "numbers.isEmpty()" in erste && "return" in erste,
        )
    }
}
