package org.biglau.res

import org.biglau.Quelltext
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

    /** Der Name der App wird nicht übersetzt - er ist in jeder Sprache derselbe. */
    private val absichtlichNurEnglisch = setOf("app_name")

    private fun keys(dir: String, tag: String): Set<String> {
        val dateien = Quelltext.texte(dir, if (tag == "plurals") "plurals.xml" else "strings.xml")
        assertTrue("$dir/$tag fehlt in jedem Modul", dateien.isNotEmpty())
        return dateien.flatMap { datei ->
            Regex("<$tag name=\"([^\"]+)\"").findAll(datei.readText()).map { it.groupValues[1] }
        }.toSet()
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
            val text = Quelltext.texte(dir).joinToString("\n") { it.readText() }
            val leer = Regex("<string name=\"([^\"]+)\"></string>").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("leere Texte in $dir", emptyList<String>(), leer)
        }
    }

    /**
     * Jedes Modul mit Texten hat beide Sprachen.
     *
     * Beim Umzug von sieben Texten nach `core:ui` am 3.9.2026 ist dort zum ersten Mal ein
     * `values-de` entstanden. Hätte ich es vergessen, wäre die Oberfläche auf einem
     * deutschen Telefon an diesen Stellen englisch geblieben — die Schlüsselvergleiche oben
     * hätten es gemeldet, aber als Liste fehlender Schlüssel, nicht als das, was es ist.
     * Diese Regel sagt es beim Namen des Moduls.
     */
    @Test
    fun `jedes Modul mit Texten hat beide Sprachen`() {
        val englisch = Quelltext.texte("values").map { it.parentFile.parentFile.parentFile }
        val deutsch = Quelltext.texte("values-de").map { it.parentFile.parentFile.parentFile }
        assertEquals(
            "Ein Modul hat englische Texte und keine deutschen",
            englisch.map { it.canonicalPath }.sorted(),
            deutsch.map { it.canonicalPath }.sorted(),
        )
    }

    @Test
    fun `Platzhalter stimmen ueberein`() {
        // Ein %1$s auf Deutsch und keiner auf Englisch wirft zur Laufzeit - und zwar erst
        // auf dem Geraet mit der anderen Sprache.
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun placeholders(dir: String): Map<String, Int> =
            pattern.findAll(Quelltext.texte(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to Regex("%\\d+\\$[sd]").findAll(m.groupValues[2]).count() }
        val de = placeholders("values-de")
        val en = placeholders("values")
        val abweichend = en.filter { (key, count) -> de[key] != null && de[key] != count }.keys
        assertEquals("unterschiedlich viele Platzhalter", emptySet<String>(), abweichend)
    }

    /**
     * Ein längerer Text, der in beiden Dateien wortgleich steht, ist fast immer eine
     * vergessene Übersetzung. Kurze Wörter wie „SOS" oder „OK" dürfen gleich sein.
     *
     * Diese Prüfung stand bis zum 3.9.2026 in einer zweiten Klasse `TranslationTest`, deren
     * drei andere Prüfungen wortgleich hier schon standen. Zwei Stellen, die dasselbe
     * zählen, sind keine doppelte Sicherheit: sie sind der Ort, an dem eines Tages die eine
     * repariert wird und die andere nicht.
     */
    @Test
    fun `kein laengerer Text steht unuebersetzt da`() {
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun texte(dir: String): Map<String, String> =
            pattern.findAll(Quelltext.texte(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to m.groupValues[2] }
        val en = texte("values")
        val de = texte("values-de")
        val gleich = en.keys.intersect(de.keys).filter { name ->
            en.getValue(name).length > 12 && en.getValue(name) == de.getValue(name)
        }
        assertEquals("wortgleich in beiden Sprachen - übersetzt?", emptyList<String>(), gleich)
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
        val dateien = Quelltext.texte(verzeichnis)
        return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(dateien.joinToString("\n") { it.readText() })
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
