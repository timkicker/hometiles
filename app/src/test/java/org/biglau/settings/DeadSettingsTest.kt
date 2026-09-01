package org.biglau.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kein Schalter im Modell, den niemand liest.
 *
 * Vier Felder in `Behaviour` und `Security` standen seit dem ersten Tag da und wurden
 * nirgends benutzt. Ein toter Schalter ist schlimmer als ein fehlender: er steht in der
 * gesicherten Konfiguration, sieht nach einer Zusage aus und tut nichts. Aufgefallen beim
 * Abgleich von `PLAN.md` 4.4 gegen das Gebaute.
 */
class DeadSettingsTest {

    private val quelle = File("src/main/java/org/biglau")

    /**
     * Felder, deren Umsetzung noch aussteht. Wer eines umsetzt, streicht es hier - und wer
     * ein neues Feld anlegt, ohne es zu benutzen, bekommt hier einen roten Test.
     */
    private val nochNichtUmgesetzt = setOf("swipeBetweenScreens")

    private fun benutztAusserhalbDesModells(feld: String): Int =
        quelle.walkTopDown()
            .filter { it.extension == "kt" && it.name != "Model.kt" }
            .sumOf { datei -> datei.readLines().count { it.contains(feld) } }

    @Test
    fun `jedes Verhaltensfeld wird auch gelesen`() {
        val felder = Regex("""val (\w+): \w+""")
            .findAll(modellAbschnitt("data class Behaviour("))
            .map { it.groupValues[1] }
            .toList()
        val tot = felder.filter { it !in nochNichtUmgesetzt && benutztAusserhalbDesModells(it) == 0 }
        assertEquals(emptyList<String>(), tot)
    }

    @Test
    fun `jedes Sicherheitsfeld wird auch gelesen`() {
        val felder = Regex("""val (\w+): [\w?]+""")
            .findAll(modellAbschnitt("data class Security("))
            .map { it.groupValues[1] }
            .toList()
        val tot = felder.filter { it !in nochNichtUmgesetzt && benutztAusserhalbDesModells(it) == 0 }
        assertEquals(emptyList<String>(), tot)
    }

    @Test
    fun `die Ausnahmeliste bleibt kurz`() {
        // Sie ist eine Merkliste, keine Ablage. Wächst sie, ist der Plan weiter weg vom
        // Gebauten als gedacht.
        assertEquals(true, nochNichtUmgesetzt.size <= 3)
    }

    private fun modellAbschnitt(kopf: String): String {
        val text = File(quelle, "data/Model.kt").readText()
        val start = text.indexOf(kopf)
        return text.substring(start, text.indexOf(")", start))
    }
}
