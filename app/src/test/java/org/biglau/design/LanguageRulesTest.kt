package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Sprachregeln aus PLAN.md 3.7.
 *
 * „Knöpfe benennen die Handlung: ‚Anrufen', nicht ‚Los'." Die Bestätigung beim Löschen der
 * Anrufliste stand auf „Ja" und „Nein" - Wörter, die beide alles heißen können, unter einer
 * Frage, deren falsche Antwort nicht rückgängig zu machen ist.
 */
class LanguageRulesTest {

    private val sprachen = listOf("values", "values-de")

    private fun texte(verzeichnis: String): Map<String, String> {
        val datei = File("src/main/res/$verzeichnis/strings.xml")
        return Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(datei.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun verstoesse(muster: Regex): List<String> =
        sprachen.flatMap { verzeichnis ->
            texte(verzeichnis).filterValues { muster.containsMatchIn(it) }.keys.map { "$verzeichnis/$it" }
        }

    @Test
    fun `kein Ausrufezeichen`() {
        // Ein Ausrufezeichen ist entweder Werbung oder Panik. Beides ist hier fehl am Platz.
        assertEquals(emptyList<String>(), verstoesse(Regex("!")))
    }

    @Test
    fun `keine Entschuldigungen`() {
        assertEquals(
            emptyList<String>(),
            verstoesse(Regex("""(?i)\b(sorry|oops|ups|entschuldig\w*|leider)\b""")),
        )
    }

    @Test
    fun `keine Knoepfe ohne Handlung`() {
        // Ganze Texte, die nur aus einem nichtssagenden Wort bestehen. Ein "Ja" unter einer
        // Löschfrage sagt nicht, was es löscht.
        val leer = Regex("""^(OK|Ja|Nein|Yes|No|Los|Go|Weiter\.\.\.)$""")
        val treffer = sprachen.flatMap { verzeichnis ->
            texte(verzeichnis).filterValues { leer.matches(it.trim()) }.keys.map { "$verzeichnis/$it" }
        }
        assertEquals(emptyList<String>(), treffer)
    }

    @Test
    fun `Fertig und Weiter bleiben erlaubt`() {
        // Gegenprobe zur Regel oben: "Fertig" schließt einen Vorgang ab und benennt damit
        // sehr wohl eine Handlung. Die Regel zielt auf Antworten ohne Verb, nicht auf kurze
        // Wörter an sich - sonst würde sie am Ziel vorbeischießen.
        val alle = texte("values-de").values.map { it.trim() }
        assertEquals(true, alle.contains("Fertig"))
    }

    @Test
    fun `leere Zustaende sagen, was zu tun ist`() {
        // Die leere Kachel ist der häufigste leere Zustand der ganzen App.
        val de = texte("values-de")
        val en = texte("values")
        assertEquals(true, de.containsKey("empty_tile_invite"))
        assertEquals(true, en.containsKey("empty_tile_invite"))
        assertEquals(true, de.getValue("empty_tile_invite").contains("tippen", ignoreCase = true))
        assertEquals(true, en.getValue("empty_tile_invite").contains("tap", ignoreCase = true))
    }
}

/**
 * Deutsche Texte, die im Quelltext festhängen statt in den Ressourcen zu stehen.
 *
 * Zweimal passiert: der Notfall-Bildschirm und die Diagnose-Seite. Beide sind ausgerechnet
 * die Seiten, die jemand aufschlägt, wenn etwas nicht geht - und auf einem englischen Gerät
 * waren sie dann deutsch. Der Sprachdateien-Vergleich findet das nicht: was gar nicht in
 * strings.xml steht, fehlt dort auch nicht.
 */
class HardcodedGermanTest {

    private val deutscheWorte = Regex(
        """"[^"]*\b(nicht|keine|keiner|Gerät|Fenster|Dichte|Schrift|Nutzbar|Anrufe|Kontakte|""" +
            """Startbildschirm|Kachel|Kacheln|Bildschirm|Einstellungen|Fehler|Absturz)\b[^"]*"""",
    )

    @Test
    fun `keine deutschen Zeichenketten im Quelltext`() {
        val treffer = File("src/main/java/org/biglau").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, roh ->
                    val zeile = roh.trim()
                    // Kommentare sind absichtlich deutsch, Importe und Anmerkungen egal.
                    val entwicklerMeldung = zeile.startsWith("require(") ||
                        zeile.startsWith("check(") || zeile.startsWith("error(")
                    if (zeile.startsWith("//") || zeile.startsWith("*") || zeile.startsWith("/*")) {
                        null
                    } else if (entwicklerMeldung) {
                        // Zusicherungen im Code sieht nie ein Nutzer, sondern nur, wer den
                        // Quelltext liest - und der ist hier durchgehend deutsch.
                        null
                    } else if (deutscheWorte.containsMatchIn(zeile)) {
                        "${datei.name}:${index + 1}: $zeile"
                    } else {
                        null
                    }
                }
            }.toList()
        assertEquals(emptyList<String>(), treffer)
    }
}
