package org.biglau.design

import java.io.File
import org.biglau.Quelltext
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

    /**
     * Alle Texte einer Sprache — **auch die Mehrzahlformen**.
     *
     * Die erste Fassung las nur `strings.xml`. Was in `plurals.xml` steht, steht genauso auf
     * dem Bildschirm („Drei Bildschirme, vierzehn Kacheln, zwei Ordner gehen verloren") und
     * war von jeder Sprachregel hier ausgenommen, ohne dass es irgendwo stand. Am 3.9.2026
     * nachgezogen.
     */
    private fun texte(verzeichnis: String): Map<String, String> {
        val dateien = Quelltext.texte(verzeichnis) + Quelltext.texte(verzeichnis, "plurals.xml")
        val roh = dateien.joinToString("\n") { it.readText() }
        val einzel = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(roh)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val mehrzahl = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(roh)
            .flatMap { treffer ->
                Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(treffer.groupValues[2])
                    .map { "${treffer.groupValues[1]}/${it.groupValues[1]}" to it.groupValues[2] }
            }
            .toMap()
        return einzel + mehrzahl
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

    /**
     * Leere Zustände auf Bildschirmen, auf denen jemand landet und wartet.
     *
     * Bis zum 3.9.2026 prüfte diese Regel genau **einen** Text — die leere Kachel — und hiess
     * trotzdem „leere Zustände". Nachgesehen: die leere Anrufliste und die leere
     * Nachrichtenliste sagten nur, dass nichts da ist. Beide sagen jetzt auch, was als
     * Nächstes kommt; geprüft wird das an zwei Sätzen. Eine Regel über Prosa ist grob, aber
     * sie fängt den Rückfall in den blossen Befund.
     */
    @Test
    fun `leere Zustaende sagen, was zu tun ist`() {
        val de = texte("values-de")
        val en = texte("values")
        assertEquals(true, de.containsKey("empty_tile_invite"))
        assertEquals(true, en.containsKey("empty_tile_invite"))
        assertEquals(true, de.getValue("empty_tile_invite").contains("tippen", ignoreCase = true))
        assertEquals(true, en.getValue("empty_tile_invite").contains("tap", ignoreCase = true))

        val landeplaetze = listOf("calllog_empty", "sms_empty", "apps_recent_none", "favourites_none")
        listOf("values" to en, "values-de" to de).forEach { (verzeichnis, alle) ->
            val bloss = landeplaetze.filter { name ->
                val text = alle[name] ?: return@filter false
                text.trim().count { it == '.' } < 2
            }
            assertEquals(
                "$verzeichnis: nur ein Satz - was ist, aber nicht, was als Nächstes kommt",
                emptyList<String>(),
                bloss,
            )
        }
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

    // Ohne IGNORE_CASE lief "Keine passende App gefunden" glatt durch: das Muster kannte
    // nur das kleine "keine". Ein Test, der nur die Kleinschreibung sieht, findet
    // ausgerechnet die Satzanfaenge nicht - und Meldungen fangen mit einem Satz an.
    private val deutscheWorte = Regex(
        """"[^"]*\b(nicht|keine|keiner|kein|Gerät|Fenster|Dichte|Schrift|Nutzbar|Anrufe|""" +
            """Kontakte|Startbildschirm|Kachel|Kacheln|Bildschirm|Einstellungen|Fehler|""" +
            """Absturz|Berechtigung|gefunden|fehlt|passende|Mobil|Privat|Arbeit)\b[^"]*"""",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun `keine deutschen Zeichenketten im Quelltext`() {
        val treffer = Quelltext.dateien().asSequence()
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

    /**
     * Der Wortliste oben entkommt jeder deutsche Satz, der zufaellig kein Wort daraus
     * enthaelt. Die schaerfere Frage stellt sich anders herum: eine Meldung an den Nutzer
     * darf ueberhaupt kein Text im Quelltext sein, egal in welcher Sprache. Sie gehoert in
     * strings.xml, sonst gibt es sie nur einmal.
     */
    @Test
    fun `keine Meldung mit festem Text`() {
        val fest = Regex("""Notice\.show\([^,]+,\s*"""")
        val treffer = Quelltext.dateien().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, zeile ->
                    if (fest.containsMatchIn(zeile)) "${datei.name}:${index + 1}" else null
                }
            }.toList()
        assertEquals(emptyList<String>(), treffer)
    }

    /**
     * Und: Meldungen laufen ueber [org.biglau.ui.Notice], nicht an ihm vorbei. Wer einen
     * Toast direkt baut, umgeht die Einstellung "Meldungen warten, bis du sie wegtippst" -
     * und ausgerechnet die Meldung, auf die es ankommt, blitzt dann doch nur auf.
     */
    @Test
    fun `kein Toast am Notice vorbei`() {
        val treffer = Quelltext.dateien().asSequence()
            .filter { it.extension == "kt" && it.name != "Notice.kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, zeile ->
                    if (zeile.contains("Toast.makeText")) "${datei.name}:${index + 1}" else null
                }
            }.toList()
        assertEquals(emptyList<String>(), treffer)
    }
}

/**
 * Die Sprachnamen selbst werden nicht übersetzt.
 *
 * Wer die eingestellte Sprache nicht liest, sucht in der Liste nach dem Wort, das er
 * kennt. "Deutsch" als "German" zu übersetzen macht die Zeile genau für den unlesbar,
 * der sie braucht.
 */
class LanguageNamesTest {

    private fun wert(verzeichnis: String, name: String): String {
        val dateien = Quelltext.texte(verzeichnis)
        return Regex("""<string name="$name">(.*?)</string>""")
            .find(dateien.joinToString("\n") { it.readText() })!!
            .groupValues[1]
    }

    @Test
    fun `die sprachnamen stehen in beiden dateien gleich`() {
        assertEquals(wert("values", "language_german"), wert("values-de", "language_german"))
        assertEquals(wert("values", "language_english"), wert("values-de", "language_english"))
    }

    @Test
    fun `sie stehen in ihrer eigenen sprache`() {
        assertEquals("Deutsch", wert("values", "language_german"))
        assertEquals("English", wert("values-de", "language_english"))
    }
}

/**
 * Datum und Uhrzeit folgen der eingestellten Sprache, nicht der des Prozesses.
 *
 * Nach dem Umstellen auf Deutsch stand ueber dem Startbildschirm weiter "Tue, 1. Sep":
 * die Texte kamen aus den Ressourcen und waren deutsch, der Wochentag kam aus
 * `Locale.getDefault()` und blieb englisch. Halb uebersetzt ist schlechter als gar nicht -
 * es sieht nach einem Fehler aus, und man sucht ihn bei sich.
 */
class DateLocaleTest {

    @Test
    fun `keine anzeige formatiert mit der prozesssprache`() {
        val treffer = Quelltext.dateien().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, zeile ->
                    // Der Sprecher darf darauf zurueckfallen, wenn die App der Sprache des
                    // Telefons folgt - eine Stimme ohne Sprache spricht gar nicht.
                    val rueckfall = zeile.contains("?: Locale.getDefault()")
                    if (zeile.contains("Locale.getDefault()") && !rueckfall) {
                        "${datei.name}:${index + 1}"
                    } else {
                        null
                    }
                }
            }.toList()
        assertEquals(emptyList<String>(), treffer)
    }

    /**
     * Gegenstueck: Locale.US bleibt erlaubt und ist an drei Stellen sogar noetig - im
     * Absturzprotokoll, im Dateinamen der Sicherung und in den Koordinaten der SOS-
     * Nachricht. Dort wuerde eine deutsche Sprache aus "48.20849" ein "47,26543" machen,
     * und der Kartenlink der Rettung waere kaputt.
     */
    @Test
    fun `Locale US bleibt fuer maschinentexte`() {
        val mitUS = Quelltext.dateien().asSequence()
            .filter { it.extension == "kt" }
            .count { it.readText().contains("Locale.US") }
        assertEquals(true, mitUS >= 3)
    }
}
