package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Angaben für F-Droid — vollständig, in beiden Sprachen, und nicht abgeschrieben.
 *
 * Was hier schiefgeht, sieht niemand beim Bauen: die Auflistung im Laden entsteht aus
 * Dateien, die kein Übersetzer anfasst. Am 3.9.2026 gefunden: die **deutschen**
 * Bildschirmfotos waren Byte für Byte die englischen. Für eine App, deren ganzer Zweck ist,
 * dass jemand die Sprache lesen kann, ist das der falscheste Fehler von allen — wer sie in
 * F-Droid ansieht, sieht eine fremde Sprache und geht weiter.
 *
 * Ebenso still: eine neue Fassung ohne `changelogs/<versionCode>.txt`. F-Droid zeigt dann
 * gar nichts an, ohne sich zu beschweren.
 */
class LadenTexteTest {

    private val wurzel = File("../fastlane/metadata/android")
    private val sprachen = listOf("de-DE", "en-US")

    private fun datei(sprache: String, name: String) = File(wurzel, "$sprache/$name")

    private fun bilder(sprache: String): List<File> =
        File(wurzel, "$sprache/images/phoneScreenshots")
            .listFiles { d -> d.extension == "png" }?.sortedBy { it.name }.orEmpty()

    @Test
    fun `beide sprachen haben dieselben angaben`() {
        sprachen.forEach { sprache ->
            listOf("title.txt", "short_description.txt", "full_description.txt").forEach {
                assertTrue("$sprache/$it fehlt", datei(sprache, it).isFile)
            }
        }
    }

    @Test
    fun `die kurzbeschreibung passt in die vorgegebene laenge`() {
        sprachen.forEach { sprache ->
            val text = datei(sprache, "short_description.txt").readText().trim()
            assertTrue("$sprache: Kurzbeschreibung leer", text.isNotEmpty())
            assertTrue(
                "$sprache: Kurzbeschreibung ist ${text.length} Zeichen lang, F-Droid " +
                    "schneidet bei 80 ab",
                text.length <= 80,
            )
            val lang = datei(sprache, "full_description.txt").readText().trim()
            assertTrue("$sprache: Beschreibung ist zu lang (${lang.length} > 4000)", lang.length <= 4000)
        }
    }

    @Test
    fun `zur gebauten fassung gibt es einen aenderungstext`() {
        val bau = File("build.gradle.kts").readText()
        val version = Regex("""versionCode\s*=\s*(\d+)""").find(bau)?.groupValues?.get(1)
        assertTrue("versionCode nicht gefunden", version != null)
        sprachen.forEach { sprache ->
            val eintrag = datei(sprache, "changelogs/$version.txt")
            assertTrue(
                "$sprache: kein Änderungstext für versionCode $version. F-Droid zeigt " +
                    "dann nichts an und sagt nichts dazu.",
                eintrag.isFile && eintrag.readText().isNotBlank(),
            )
        }
    }

    @Test
    fun `die bildschirmfotos sind luecklos durchnummeriert`() {
        sprachen.forEach { sprache ->
            val namen = bilder(sprache).map { it.nameWithoutExtension }
            assertTrue("$sprache: keine Bildschirmfotos", namen.isNotEmpty())
            assertEquals(
                "$sprache: Lücke in der Nummerierung",
                (1..namen.size).map { it.toString() },
                namen.sortedBy { it.toIntOrNull() ?: 0 },
            )
        }
    }

    @Test
    fun `die deutschen bildschirmfotos zeigen deutsch`() {
        val de = bilder("de-DE").associate { it.name to it.readBytes().toList() }
        val en = bilder("en-US").associate { it.name to it.readBytes().toList() }
        assertEquals("verschieden viele Bildschirmfotos je Sprache", en.keys, de.keys)
        val gleich = de.filter { (name, inhalt) -> en[name] == inhalt }.keys.sorted()
        assertEquals(
            "Diese deutschen Bildschirmfotos sind Byte für Byte die englischen. Wer die " +
                "App in F-Droid ansieht, sieht eine Sprache, die er vielleicht nicht liest.",
            emptyList<String>(),
            gleich,
        )
    }
}
