package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every language knows the same texts as english.
 *
 * a missing key otherwise shows up first to whoever needs it least: the emergency screen
 * stood fixed in german in the source, unreadable on an english phone.
 */
class TranslationsTest {

    /** the app's name is the same in every language. */
    private val onlyEnglishOnPurpose = setOf("app_name")

    /** searched, not listed: a third language would be seen by no rule here. */
    private val translated = Quelltext.translations()

    /**
     * only what ships has to be complete. a language in progress stands in the tree but not
     * in `resourceConfigurations`, and android falls back to english for it. everything else
     * here holds for it too.
     */
    private val complete = Quelltext.shipped().filter { it != "values" }

    private fun keys(dir: String, tag: String): Set<String> {
        val files = Quelltext.texts(dir, if (tag == "plurals") "plurals.xml" else "strings.xml")
        assertTrue("$dir/$tag is missing in every module", files.isNotEmpty())
        return files.flatMap { file ->
            Regex("<$tag name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }
        }.toSet()
    }

    @Test
    fun `every translated text has an english one`() {
        val en = keys("values", "string")
        translated.forEach { language ->
            assertEquals("only present in $language", emptySet<String>(), keys(language, "string") - en)
        }
    }

    @Test
    fun `every english text is translated`() {
        val en = keys("values", "string")
        complete.forEach { language ->
            assertEquals(
                "missing in $language",
                emptySet<String>(),
                en - keys(language, "string") - onlyEnglishOnPurpose,
            )
        }
    }

    @Test
    fun `the plural forms stand in every language too`() {
        complete.forEach { language ->
            assertEquals("plural forms in $language", keys("values", "plurals"), keys(language, "plurals"))
        }
    }

    /**
     * the other direction: the rules above only check directory to shipping list, so without
     * this one a language named in `resourceConfigurations` could have no texts at all.
     */
    @Test
    fun `every shipped language also stands in the tree`() {
        assertEquals(
            "resourceConfigurations names a language that has no texts",
            emptyList<String>(),
            Quelltext.shipped().filterNot { it in Quelltext.languages() },
        )
    }

    @Test
    fun `no text is empty`() {
        Quelltext.languages().forEach { dir ->
            val text = Quelltext.texts(dir).joinToString("\n") { it.readText() }
            val empty = Regex("<string name=\"([^\"]+)\"></string>").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("empty texts in $dir", emptyList<String>(), empty)
        }
    }

    /**
     * says it by the module's name. the key comparisons above would report the same fault as
     * a list of missing keys, which is not what it is.
     */
    @Test
    fun `every module with texts has both languages`() {
        val english = Quelltext.texts("values").map { it.parentFile.parentFile.parentFile }
        complete.forEach { language ->
            assertEquals(
                "a module has english texts and none in $language",
                english.map { it.canonicalPath }.sorted(),
                Quelltext.texts(language).map { it.parentFile.parentFile.parentFile.canonicalPath }.sorted(),
            )
        }
    }

    @Test
    fun `placeholders match`() {
        // a %1$s on one side and none on the other throws at runtime, and only on the phone
        // set to the other language.
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun placeholders(dir: String): Map<String, Int> =
            pattern.findAll(Quelltext.texts(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to Regex("%\\d+\\$[sd]").findAll(m.groupValues[2]).count() }
        val en = placeholders("values")
        translated.forEach { language ->
            val other = placeholders(language)
            val differing = en.filter { (key, count) -> other[key] != null && other[key] != count }.keys
            assertEquals("different number of placeholders in $language", emptySet<String>(), differing)
        }
    }

    /**
     * a word list is read by **index**, `tile_colors[colorIndex]`: a missing entry shifts
     * every access by one and the last one reaches into nothing.
     *
     * a word list also falls back as a **whole** or not at all, unlike texts and plural
     * forms, so this holds for a language in progress as well: all of it or none.
     */
    @Test
    fun `word lists are the same length everywhere`() {
        fun lists(dir: String): Map<String, Int> =
            Regex("""<string-array name="([^"]+)">(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(Quelltext.texts(dir).joinToString("\n") { it.readText() })
                .associate { it.groupValues[1] to Regex("<item>").findAll(it.groupValues[2]).count() }
        val en = lists("values")
        assertTrue("there is no word list left at all - then this rule checks nothing", en.isNotEmpty())
        translated.forEach { language ->
            val other = lists(language)
            val wrong = en.keys.intersect(other.keys)
                .filter { en[it] != other[it] }
                .map { "$language: $it has ${other[it]} instead of ${en[it]} entries" }
            assertEquals("a word list has a different length", emptyList<String>(), wrong)
        }
    }

    /**
     * the rule above reads only `<string>`, yet a missing `%1$d` is *more* likely in a plural
     * form: the one-form often writes the number out and the other-form needs it.
     *
     * compared form against the same form: that `one` and `other` differ is correct.
     */
    @Test
    fun `placeholders match in the plural forms too`() {
        fun forms(dir: String): Map<Pair<String, String>, Set<String>> {
            val content = Quelltext.texts(dir, "plurals.xml").joinToString("\n") { it.readText() }
            return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(content)
                .flatMap { entry ->
                    Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                        .findAll(entry.groupValues[2])
                        .map { piece ->
                            (entry.groupValues[1] to piece.groupValues[1]) to
                                Regex("""%\d+\$[sd]""").findAll(piece.groupValues[2])
                                    .map { it.value }.toSet()
                        }
                }.toMap()
        }
        val en = forms("values")
        translated.forEach { language ->
            val other = forms(language)
            val differing = en.keys.intersect(other.keys)
                .filter { en[it] != other[it] }
                .map { (name, amount) -> "$language: $name/$amount ${en[name to amount]} instead of ${other[name to amount]}" }
            assertEquals("placeholders in a plural form", emptyList<String>(), differing)
        }
    }

    /**
     * a longer text standing word for word in both files is nearly always a forgotten
     * translation. short words like "SOS" or "OK" may be the same.
     */
    @Test
    fun `no longer text stands there untranslated`() {
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun texts(dir: String): Map<String, String> =
            pattern.findAll(Quelltext.texts(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to m.groupValues[2] }
        val en = texts("values")
        translated.forEach { language ->
            val other = texts(language)
            val same = en.keys.intersect(other.keys).filter { name ->
                en.getValue(name).length > 12 && en.getValue(name) == other.getValue(name)
            }
            assertEquals("word for word the english in $language - translated?", emptyList<String>(), same)
        }
    }
}

/**
 * numbers in words that fit the number.
 *
 * the question before resetting said "1 folders", and a warning that reads sloppily is the
 * one warning nobody takes seriously.
 */
class PluralsTest {

    private fun plurals(directory: String): Map<String, Set<String>> {
        val files = Quelltext.texts(directory)
        return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(files.joinToString("\n") { it.readText() })
            .associate { hit ->
                hit.groupValues[1] to Regex("""quantity="([^"]+)"""")
                    .findAll(hit.groupValues[2])
                    .map { it.groupValues[1] }
                    .toSet()
            }
    }

    @Test
    fun `every language knows the same plurals`() {
        Quelltext.shipped().filter { it != "values" }.forEach { language ->
            assertEquals("plural forms in $language", plurals("values").keys, plurals(language).keys)
        }
    }

    @Test
    fun `every plural has a singular and a plural form`() {
        val incomplete = (plurals("values") + plurals("values-de"))
            .filterValues { !it.containsAll(setOf("one", "other")) }
            .keys
        assertEquals(emptySet<String>(), incomplete)
    }
}
