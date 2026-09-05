package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the language rules from `PLAN.md` 3.7: buttons name the action.
 *
 * the confirmation before deleting the call log read yes and no, words that can mean
 * anything, under a question whose wrong answer cannot be undone.
 */
class LanguageRulesTest {

    private val sprachen = listOf("values", "values-de")

    /**
     * every text of a language, *including the plurals*: reading only `strings.xml` left
     * everything in `plurals.xml` outside all these rules, unmentioned.
     */
    private fun texte(verzeichnis: String): Map<String, String> {
        val dateien = Quelltext.texts(verzeichnis) + Quelltext.texts(verzeichnis, "plurals.xml")
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
        // an exclamation mark is either advertising or panic.
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
        // whole texts made of one empty word: a yes under a delete question does not say
        // what it deletes.
        val leer = Regex("""^(OK|Ja|Nein|Yes|No|Los|Go|Weiter\.\.\.)$""")
        val treffer = sprachen.flatMap { verzeichnis ->
            texte(verzeichnis).filterValues { leer.matches(it.trim()) }.keys.map { "$verzeichnis/$it" }
        }
        assertEquals(emptyList<String>(), treffer)
    }

    @Test
    fun `Fertig und Weiter bleiben erlaubt`() {
        // the counter-check: done closes something and does name an action. the rule aims
        // at answers without a verb, not at short words as such.
        val alle = texte("values-de").values.map { it.trim() }
        assertEquals(true, alle.contains("Fertig"))
    }

    /**
     * empty states on screens someone lands on and waits.
     *
     * the rule checked exactly *one* text and was still called empty states. a rule about
     * prose is coarse, but it catches the fall back into the bare finding.
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
 * german texts stuck in the source instead of the resources.
 *
 * it happened twice, on the safe-mode screen and the diagnostics page, which are exactly
 * the pages one opens when something is wrong. comparing the resource files does not find
 * it: what is not in `strings.xml` is not missing there either.
 */
class HardcodedGermanTest {

    // without IGNORE_CASE a capitalised sentence slipped straight through: a rule that sees
    // only lower case misses exactly the beginnings, and notices begin with a sentence.
    private val deutscheWorte = Regex(
        """"[^"]*\b(nicht|keine|keiner|kein|Gerät|Fenster|Dichte|Schrift|Nutzbar|Anrufe|""" +
            """Kontakte|Startbildschirm|Kachel|Kacheln|Bildschirm|Einstellungen|Fehler|""" +
            """Absturz|Berechtigung|gefunden|fehlt|passende|Mobil|Privat|Arbeit)\b[^"]*"""",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun `keine deutschen Zeichenketten im Quelltext`() {
        val treffer = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, roh ->
                    val zeile = roh.trim()
                    // comments, imports and annotations do not count.
                    val entwicklerMeldung = zeile.startsWith("require(") ||
                        zeile.startsWith("check(") || zeile.startsWith("error(")
                    if (zeile.startsWith("//") || zeile.startsWith("*") || zeile.startsWith("/*")) {
                        null
                    } else if (entwicklerMeldung) {
                        // assertions are never seen by a user, only by whoever reads the
                        // source.
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
     * any sentence without a word from the list above escapes it. the sharper question is the
     * other way round: a notice must not be text in the source at all, in any language.
     */
    @Test
    fun `keine Meldung mit festem Text`() {
        val fest = Regex("""Notice\.show\([^,]+,\s*"""")
        val treffer = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, zeile ->
                    if (fest.containsMatchIn(zeile)) "${datei.name}:${index + 1}" else null
                }
            }.toList()
        assertEquals(emptyList<String>(), treffer)
    }

    /**
     * notices go through [org.biglau.ui.Notice] and not past it: building a toast directly
     * bypasses the setting that keeps notices standing until tapped away.
     */
    @Test
    fun `kein Toast am Notice vorbei`() {
        val treffer = Quelltext.files().asSequence()
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
 * the language names themselves are not translated: whoever cannot read the current one
 * looks for the word they know, and translating it makes the row unreadable to them.
 */
class LanguageNamesTest {

    private fun wert(verzeichnis: String, name: String): String {
        val dateien = Quelltext.texts(verzeichnis)
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
 * date and time follow the chosen language, not the process one.
 *
 * the texts came from the resources while the weekday came from `Locale.getDefault()`, so
 * the header read half in one language and half in the other. half translated is worse than
 * not at all: it looks like a fault, and one looks for it in oneself.
 */
class DateLocaleTest {

    @Test
    fun `keine anzeige formatiert mit der prozesssprache`() {
        val treffer = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { datei ->
                datei.readLines().asSequence().mapIndexedNotNull { index, zeile ->
                    // the speaker may fall back on it when the app follows the phone's
                    // language: a voice without a language does not speak.
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
     * the counterpart: Locale.US stays allowed and is needed in three places, the crash log,
     * the backup file name and the sos coordinates, where another language would turn the
     * decimal point into a comma and break the rescue map link.
     */
    @Test
    fun `Locale US bleibt fuer maschinentexte`() {
        val mitUS = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .count { it.readText().contains("Locale.US") }
        assertEquals(true, mitUS >= 3)
    }
}
