package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every text has to stand somewhere.
 *
 * "delete this entry" lay translated in both languages and was never shown by any line -
 * deleting a single call was reachable by long press alone, and that stood nowhere. a text
 * without a place is either a leftover or a promise the surface does not keep; neither shows
 * on its own, because an unused text breaks nothing - it is only missing where it belonged.
 *
 * checked against the english set, since both languages carry the same keys (see
 * [TranslationsTest]); a german leftover would show up there.
 */
class UnusedStringsTest {

    private val res = Quelltext.resRoots

    /**
     * names that rightly have no place in the source.
     *
     * the list would stay empty today - `app_name` stands in the manifest and is found there
     * through `@string/`. it stands here so a future special case has to be named instead of
     * the rule being loosened.
     */
    private val withoutAPlace = emptySet<String>()

    private fun names(tag: String): List<String> {
        val files = Quelltext.texts("values", if (tag == "plurals") "plurals.xml" else "strings.xml")
        assertTrue("values/$tag is missing in every module", files.isNotEmpty())
        return files.flatMap { file ->
            Regex("<$tag name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }
        }.toList()
    }

    /** everything pointing at a resource name: `R.string.x`, `R.plurals.x`, `@string/x`. */
    private fun used(): Set<String> {
        val hits = mutableSetOf<String>()
        val pointer = Regex("""R\.(?:string|plurals)\.([A-Za-z0-9_]+)|@(?:string|plurals)/([A-Za-z0-9_]+)""")
        (Quelltext.roots + res + File("src/main/AndroidManifest.xml")).forEach { place ->
            place.walkTopDown().filter { it.isFile }.forEach { file ->
                pointer.findAll(file.readText()).forEach {
                    hits += it.groupValues[1].ifEmpty { it.groupValues[2] }
                }
            }
        }
        return hits
    }

    @Test
    fun `no text stands around unused`() {
        val used = used()
        val orphaned = (names("string") + names("plurals"))
            .filterNot { it in used || it in withoutAPlace }
        assertEquals(
            "nobody shows these texts - either the place they should stand is missing, or " +
                "they are a leftover and belong deleted: $orphaned",
            emptyList<String>(),
            orphaned,
        )
    }

    @Test
    fun `the rule finds an invented leftover`() {
        // without this a broken search expression would wave everything through.
        val used = used()
        assertTrue("an invented name must not count as used", "biglau_gibt_es_nicht" !in used)
    }
}
