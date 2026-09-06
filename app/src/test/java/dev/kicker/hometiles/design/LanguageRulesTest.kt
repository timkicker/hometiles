package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the language rules from `PLAN.md` 3.7: buttons name the action.
 *
 * the confirmation before deleting the call log read yes and no, words that can mean
 * anything, under a question whose wrong answer cannot be undone.
 */
class LanguageRulesTest {

    private val languages = listOf("values", "values-de")

    /**
     * every text of a language, *including the plurals*: reading only `strings.xml` left
     * everything in `plurals.xml` outside all these rules, unmentioned.
     */
    private fun texts(directory: String): Map<String, String> {
        val files = Quelltext.texts(directory) + Quelltext.texts(directory, "plurals.xml")
        val raw = files.joinToString("\n") { it.readText() }
        val single = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(raw)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val plural = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(raw)
            .flatMap { hits ->
                Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(hits.groupValues[2])
                    .map { "${hits.groupValues[1]}/${it.groupValues[1]}" to it.groupValues[2] }
            }
            .toMap()
        return single + plural
    }

    private fun offenders(pattern: Regex): List<String> =
        languages.flatMap { directory ->
            texts(directory).filterValues { pattern.containsMatchIn(it) }.keys.map { "$directory/$it" }
        }

    @Test
    fun `no exclamation mark`() {
        // an exclamation mark is either advertising or panic.
        assertEquals(emptyList<String>(), offenders(Regex("!")))
    }

    @Test
    fun `no apologies`() {
        assertEquals(
            emptyList<String>(),
            offenders(Regex("""(?i)\b(sorry|oops|ups|entschuldig\w*|leider)\b""")),
        )
    }

    @Test
    fun `no buttons without an action`() {
        // whole texts made of one empty word: a yes under a delete question does not say
        // what it deletes.
        val empty = Regex("""^(OK|Ja|Nein|Yes|No|Los|Go|Weiter\.\.\.)$""")
        val hits = languages.flatMap { directory ->
            texts(directory).filterValues { empty.matches(it.trim()) }.keys.map { "$directory/$it" }
        }
        assertEquals(emptyList<String>(), hits)
    }

    @Test
    fun `done and next stay allowed`() {
        // the counter-check: done closes something and does name an action. the rule aims
        // at answers without a verb, not at short words as such.
        val all = texts("values-de").values.map { it.trim() }
        assertEquals(true, all.contains("Fertig"))
    }

    /**
     * empty states on screens someone lands on and waits.
     *
     * the rule checked exactly *one* text and was still called empty states. a rule about
     * prose is coarse, but it catches the fall back into the bare finding.
     */
    @Test
    fun `empty states say what to do`() {
        val de = texts("values-de")
        val en = texts("values")
        assertEquals(true, de.containsKey("empty_tile_invite"))
        assertEquals(true, en.containsKey("empty_tile_invite"))
        assertEquals(true, de.getValue("empty_tile_invite").contains("tippen", ignoreCase = true))
        assertEquals(true, en.getValue("empty_tile_invite").contains("tap", ignoreCase = true))

        val landings = listOf("calllog_empty", "sms_empty", "apps_recent_none", "favourites_none")
        listOf("values" to en, "values-de" to de).forEach { (directory, all) ->
            val bare = landings.filter { name ->
                val text = all[name] ?: return@filter false
                text.trim().count { it == '.' } < 2
            }
            assertEquals(
                "$directory: only one sentence - what is, but not what comes next",
                emptyList<String>(),
                bare,
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

    // the german words are the check object. without IGNORE_CASE a capitalised sentence
    // slipped straight through: a rule that sees only lower case misses exactly the
    // beginnings, and notices begin with a sentence.
    private val germanWords = Regex(
        """"[^"]*\b(nicht|keine|keiner|kein|Gerät|Fenster|Dichte|Schrift|Nutzbar|Anrufe|""" +
            """Kontakte|Startbildschirm|Kachel|Kacheln|Bildschirm|Einstellungen|Fehler|""" +
            """Absturz|Berechtigung|gefunden|fehlt|passende|Mobil|Privat|Arbeit)\b[^"]*"""",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun `no german strings in the source`() {
        val hits = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, raw ->
                    val line = raw.trim()
                    // comments, imports and annotations do not count.
                    val developerMessage = line.startsWith("require(") ||
                        line.startsWith("check(") || line.startsWith("error(")
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                        null
                    } else if (developerMessage) {
                        // assertions are never seen by a user, only by whoever reads the
                        // source.
                        null
                    } else if (germanWords.containsMatchIn(line)) {
                        "${file.name}:${index + 1}: $line"
                    } else {
                        null
                    }
                }
            }.toList()
        assertEquals(emptyList<String>(), hits)
    }

    /**
     * any sentence without a word from the list above escapes it. the sharper question is the
     * other way round: a notice must not be text in the source at all, in any language.
     */
    @Test
    fun `no notice with fixed text`() {
        val fixed = Regex("""Notice\.show\([^,]+,\s*"""")
        val hits = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    if (fixed.containsMatchIn(line)) "${file.name}:${index + 1}" else null
                }
            }.toList()
        assertEquals(emptyList<String>(), hits)
    }

    /**
     * notices go through [dev.kicker.hometiles.ui.Notice] and not past it: building a toast directly
     * bypasses the setting that keeps notices standing until tapped away.
     */
    @Test
    fun `no toast past Notice`() {
        val hits = Quelltext.files().asSequence()
            .filter { it.extension == "kt" && it.name != "Notice.kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    if (line.contains("Toast.makeText")) "${file.name}:${index + 1}" else null
                }
            }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}

/**
 * the language names themselves are not translated: whoever cannot read the current one
 * looks for the word they know, and translating it makes the row unreadable to them.
 */
class LanguageNamesTest {

    private fun value(directory: String, name: String): String {
        val files = Quelltext.texts(directory)
        return Regex("""<string name="$name">(.*?)</string>""")
            .find(files.joinToString("\n") { it.readText() })!!
            .groupValues[1]
    }

    @Test
    fun `the language names read the same in both files`() {
        assertEquals(value("values", "language_german"), value("values-de", "language_german"))
        assertEquals(value("values", "language_english"), value("values-de", "language_english"))
    }

    @Test
    fun `they stand in their own language`() {
        assertEquals("Deutsch", value("values", "language_german"))
        assertEquals("English", value("values-de", "language_english"))
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
    fun `nothing on screen is formatted with the process language`() {
        val hits = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    // the speaker may fall back on it when the app follows the phone's
                    // language: a voice without a language does not speak.
                    val fallback = line.contains("?: Locale.getDefault()")
                    if (line.contains("Locale.getDefault()") && !fallback) {
                        "${file.name}:${index + 1}"
                    } else {
                        null
                    }
                }
            }.toList()
        assertEquals(emptyList<String>(), hits)
    }

    /**
     * the counterpart: Locale.US stays allowed and is needed in three places, the crash log,
     * the backup file name and the sos coordinates, where another language would turn the
     * decimal point into a comma and break the rescue map link.
     */
    @Test
    fun `Locale US stays for machine texts`() {
        val withUS = Quelltext.files().asSequence()
            .filter { it.extension == "kt" }
            .count { it.readText().contains("Locale.US") }
        assertEquals(true, withUS >= 3)
    }
}
