package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the f-droid entries - complete, in every language, and not copied.
 *
 * what goes wrong here nobody sees while building: the store listing comes from files no
 * translator touches. the **german** screenshots were byte for byte the english ones, which
 * for an app whose whole purpose is that someone can read the language is the worst mistake
 * of all. equally quiet: a new version without `changelogs/<versionCode>.txt`, where f-droid
 * shows nothing and says nothing.
 */
class StoreTextsTest {

    private val root = File("../fastlane/metadata/android")

    /**
     * the languages come from the build, not from a list here: a fixed `listOf("de-DE",
     * "en-US")` would have hidden three of the five shipped languages from the listing, and
     * no rule would have said so.
     */
    private val languages: List<String> = Regex("""resourceConfigurations \+= listOf\(([^)]*)\)""")
        .find(File("build.gradle.kts").readText())
        ?.groupValues?.get(1)
        ?.split(",")
        ?.mapNotNull { Regex(""""(\w+)"""").find(it)?.groupValues?.get(1) }
        ?.map { code -> storeName(code) }
        .orEmpty()

    /**
     * from `de` to `de-DE`: f-droid wants the country, the build does not. only the five that
     * exist, so a sixth falls over loudly instead of meaning a directory nobody created.
     */
    private fun storeName(code: String): String = when (code) {
        "en" -> "en-US"
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "es" -> "es-ES"
        "it" -> "it-IT"
        else -> throw AssertionError(
            "the language \"$code\" stands in the build, but nothing here says what its " +
                "directory under fastlane is called. f-droid wants language and country.",
        )
    }

    /** images belong to every language, not to a selection. */
    private val withImages get() = languages

    private fun file(language: String, name: String) = File(root, "$language/$name")

    private fun images(language: String): List<File> =
        File(root, "$language/images/phoneScreenshots")
            .listFiles { d -> d.extension == "png" }?.sortedBy { it.name }.orEmpty()

    /**
     * first: that anything was read at all. every rule here runs `forEach` over [languages],
     * so an empty list would let them all pass having checked nothing.
     */
    @Test
    fun `the languages really come from the build`() {
        assertTrue(
            "no language was read from resourceConfigurations. then every rule here runs " +
                "over nothing and stays green.",
            languages.size >= 2,
        )
        assertEquals(
            "the store's languages are not the build's.",
            listOf("en-US", "de-DE", "fr-FR", "es-ES", "it-IT").sorted(),
            languages.sorted(),
        )
    }

    @Test
    fun `every language has the same entries`() {
        languages.forEach { language ->
            listOf("title.txt", "short_description.txt", "full_description.txt").forEach {
                assertTrue("$language/$it is missing", file(language, it).isFile)
            }
        }
    }

    @Test
    fun `the short description fits the given length`() {
        languages.forEach { language ->
            val text = file(language, "short_description.txt").readText().trim()
            assertTrue("$language: short description empty", text.isNotEmpty())
            assertTrue(
                "$language: the short description is ${text.length} characters, f-droid cuts " +
                    "at 80",
                text.length <= 80,
            )
            val long = file(language, "full_description.txt").readText().trim()
            assertTrue("$language: description too long (${long.length} > 4000)", long.length <= 4000)
        }
    }

    @Test
    fun `the built version has a changelog`() {
        val build = File("build.gradle.kts").readText()
        val version = Regex("""versionCode\s*=\s*(\d+)""").find(build)?.groupValues?.get(1)
        assertTrue("versionCode not found", version != null)
        languages.forEach { language ->
            val entry = file(language, "changelogs/$version.txt")
            assertTrue(
                "$language: no changelog for versionCode $version. f-droid then shows " +
                    "nothing and says nothing about it.",
                entry.isFile && entry.readText().isNotBlank(),
            )
        }
    }

    /**
     * f-droid turns every line break into `<br>` and knows no markdown (their docs, read
     * 28.10.2026). the texts were wrapped at about 95 columns, so on the page each paragraph
     * broke in the middle of a sentence, and the `* ` list showed its asterisks.
     *
     * so a paragraph is one line, and a line may only follow another without a blank line
     * between them when it is a list item.
     */
    private fun layoutFaults(name: String, text: String): List<String> {
        val lines = text.trimEnd('\n').split("\n")
        val markdown = lines.filter { Regex("""^\s*[*+-] """).containsMatchIn(it) }
            .map { "$name: markdown list \"${it.take(30)}\"" }
        val broken = lines.zipWithNext()
            .filter { (one, next) -> one.isNotBlank() && next.isNotBlank() && !next.startsWith("• ") }
            .map { (one, _) -> "$name: line break inside a paragraph after \"${one.takeLast(30)}\"" }
        return markdown + broken
    }

    private fun storeTexts(): List<File> = languages.flatMap { language ->
        listOf(file(language, "full_description.txt")) +
            File(root, "$language/changelogs").listFiles().orEmpty().toList()
    }

    @Test
    fun `the store texts break only where f-droid should`() {
        val texts = storeTexts()
        assertTrue("no store text was read", texts.size > languages.size)
        assertEquals(
            "f-droid shows every line break. write a paragraph on one line and a list as " +
                "lines starting with \"• \".",
            emptyList<String>(),
            texts.flatMap { layoutFaults(it.path, it.readText()) },
        )
    }

    @Test
    fun `the layout rule finds a wrapped paragraph and a markdown list`() {
        // counter-check with the shape the texts had before 28.10.2026.
        val old = "A dialler with a keypad and its own in-call\nscreen.\n\n* Messages\n"
        assertEquals(2, layoutFaults("old", old).size)
        assertEquals(emptyList<String>(), layoutFaults("new", "One line.\n\nList:\n\n• a\n• b\n"))
    }

    @Test
    fun `the screenshots are numbered without gaps`() {
        withImages.forEach { language ->
            val names = images(language).map { it.nameWithoutExtension }
            assertTrue("$language: no screenshots", names.isNotEmpty())
            assertEquals(
                "$language: gap in the numbering",
                (1..names.size).map { it.toString() },
                names.sortedBy { it.toIntOrNull() ?: 0 },
            )
        }
    }

    /**
     * every pair, not only german against english: the rule came out of that one pair, and
     * three more languages would have added nine pairs it never looked at.
     */
    @Test
    fun `no language shows another one's images`() {
        val photos = withImages.associateWith { language ->
            images(language).associate { it.name to it.readBytes().toList() }
        }
        val pairs = withImages.flatMapIndexed { index, one ->
            withImages.drop(index + 1).map { other -> one to other }
        }
        assertTrue("too few languages with images, the rule measures nothing", pairs.isNotEmpty())
        val same = pairs.flatMap { (one, other) ->
            assertEquals(
                "$one and $other have a different number of screenshots",
                photos.getValue(one).keys,
                photos.getValue(other).keys,
            )
            photos.getValue(one)
                .filter { (name, content) -> photos.getValue(other)[name] == content }
                .keys.map { "$one/$it = $other/$it" }
        }.sorted()
        assertEquals(
            "these screenshots are byte for byte the same as another language's. whoever " +
                "looks at the app in f-droid sees a language they may not read.",
            emptyList<String>(),
            same,
        )
    }
}
