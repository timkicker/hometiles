package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jeder Bildschirm erbt von `BigLauActivity`.
 *
 * An dieser einen Stelle hängen drei Dinge, die man einzeln nie vollständig hinbekommt:
 * die **Bildschirmausrichtung** (`PLAN.md` 4.2 — vorher stand `portrait` zwölfmal im
 * Manifest), die **Sprache** (`attachBaseContext`, sonst zeigt der Bildschirm die des
 * Systems statt der eingestellten) und der **Neuaufbau**, wenn die Sprache sich ändert.
 *
 * Eine neue Activity, die `ComponentActivity` erweitert, verliert alle drei lautlos — sie
 * sieht auf dem Gerät des Entwicklers völlig richtig aus, solange dort Systemsprache und
 * Hochformat gelten. Genau diese Sorte Fehler hat in diesem Projekt schon die Palette
 * erwischt (siehe [PaletteScopeTest]).
 */
class ActivityBaseTest {

    private val quellen: List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    private fun activityZeilen(): List<Pair<String, String>> =
        quellen.flatMap { datei ->
            Regex("""^\s*(?:internal\s+)?class\s+(\w*Activity)\s*:\s*([\w.]+)""", RegexOption.MULTILINE)
                .findAll(datei.readText())
                .map { datei.name to it.groupValues[1] + " : " + it.groupValues[2] }
                .toList()
        }

    @Test
    fun `jede Activity erbt von BigLauActivity`() {
        val fremd = activityZeilen()
            .filterNot { (datei, _) -> datei == "BigLauActivity.kt" }
            .filterNot { (_, zeile) -> zeile.endsWith(": BigLauActivity") }
            .map { "${it.first}: ${it.second}" }
        assertEquals(
            "Diese Bildschirme erben nicht von BigLauActivity und verlieren damit " +
                "Ausrichtung, Sprache und den Neuaufbau bei Sprachwechsel: $fremd",
            emptyList<String>(),
            fremd,
        )
    }

    @Test
    fun `es gibt ueberhaupt Activities zu pruefen`() {
        // Sonst ginge die Regel gruen durch, weil der Suchausdruck nichts mehr findet.
        assertTrue("mindestens zehn Bildschirme", activityZeilen().size >= 10)
    }

    @Test
    fun `BigLauActivity setzt die Ausrichtung selbst`() {
        val text = quellen.first { it.name == "BigLauActivity.kt" }.readText()
        assertTrue("setzt requestedOrientation", text.contains("requestedOrientation"))
        assertTrue("setzt die Sprache", text.contains("attachBaseContext"))
    }
}
