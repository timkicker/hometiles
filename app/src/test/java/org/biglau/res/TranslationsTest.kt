package org.biglau.res

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deutsch und Englisch müssen dieselben Texte kennen.
 *
 * Anlass: der Notfall-Bildschirm - der, den jemand sieht, dessen Telefon gerade nicht mehr
 * startet - stand fest auf Deutsch im Quelltext. Auf einem englischen Gerät war ausgerechnet
 * die Rettung unlesbar. Ein fehlender Schlüssel fällt sonst erst dem Nutzer auf, und zwar
 * genau dann, wenn er ihn am wenigsten gebrauchen kann.
 */
class TranslationsTest {

    private val root = File("src/main/res")

    /** Der Name der App wird nicht übersetzt - er ist in jeder Sprache derselbe. */
    private val absichtlichNurEnglisch = setOf("app_name")

    private fun keys(dir: String, tag: String): Set<String> {
        val file = File(root, "$dir/${if (tag == "plurals") "plurals" else "strings"}.xml")
        assertTrue("$file fehlt", file.exists())
        return Regex("<$tag name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `jeder deutsche Text hat einen englischen`() {
        val de = keys("values-de", "string")
        val en = keys("values", "string")
        assertEquals("nur auf Deutsch vorhanden", emptySet<String>(), de - en)
    }

    @Test
    fun `jeder englische Text hat einen deutschen`() {
        val de = keys("values-de", "string")
        val en = keys("values", "string")
        assertEquals("nur auf Englisch vorhanden", emptySet<String>(), en - de - absichtlichNurEnglisch)
    }

    @Test
    fun `auch die Mehrzahlformen stehen in beiden Sprachen`() {
        assertEquals(keys("values", "plurals"), keys("values-de", "plurals"))
    }

    @Test
    fun `kein Text ist leer`() {
        listOf("values", "values-de").forEach { dir ->
            val text = File(root, "$dir/strings.xml").readText()
            val leer = Regex("<string name=\"([^\"]+)\"></string>").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("leere Texte in $dir", emptyList<String>(), leer)
        }
    }

    @Test
    fun `Platzhalter stimmen ueberein`() {
        // Ein %1$s auf Deutsch und keiner auf Englisch wirft zur Laufzeit - und zwar erst
        // auf dem Geraet mit der anderen Sprache.
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun placeholders(dir: String): Map<String, Int> =
            pattern.findAll(File(root, "$dir/strings.xml").readText())
                .associate { m -> m.groupValues[1] to Regex("%\\d+\\$[sd]").findAll(m.groupValues[2]).count() }
        val de = placeholders("values-de")
        val en = placeholders("values")
        val abweichend = en.filter { (key, count) -> de[key] != null && de[key] != count }.keys
        assertEquals("unterschiedlich viele Platzhalter", emptySet<String>(), abweichend)
    }
}

/**
 * Zahlen in Worten, die zur Zahl passen.
 *
 * Die Rückfrage vor dem Zurücksetzen sagte "1 folders". Wer eine Warnung schlampig
 * findet, nimmt sie nicht ernst - und das ist die eine Warnung, die man ernst nehmen muss.
 */
class PluralsTest {

    private fun plurale(verzeichnis: String): Map<String, Set<String>> {
        val datei = java.io.File("src/main/res/$verzeichnis/strings.xml")
        return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(datei.readText())
            .associate { treffer ->
                treffer.groupValues[1] to Regex("""quantity="([^"]+)"""")
                    .findAll(treffer.groupValues[2])
                    .map { it.groupValues[1] }
                    .toSet()
            }
    }

    @Test
    fun `beide sprachen kennen dieselben plurale`() {
        assertEquals(plurale("values").keys, plurale("values-de").keys)
    }

    @Test
    fun `jedes plural hat einzahl und mehrzahl`() {
        val unvollstaendig = (plurale("values") + plurale("values-de"))
            .filterValues { !it.containsAll(setOf("one", "other")) }
            .keys
        assertEquals(emptySet<String>(), unvollstaendig)
    }
}
