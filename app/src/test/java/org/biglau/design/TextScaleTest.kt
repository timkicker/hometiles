package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Einstellung heißt „Textgröße" — dann muss auch der Text mitwachsen.
 *
 * Sie hielt nur zur Hälfte, was sie versprach: Kacheln, Zeilen und Überschriften wuchsen
 * mit (`BigRow`, `BigTile` lesen `LocalTextScale`), der erklärende Text daneben nicht. Bei
 * 200 % standen große Knöpfe neben kleiner Schrift — ausgerechnet bei den Sätzen, die man
 * vergrößert lesen will.
 *
 * `bigSp` ist der Weg dorthin. **Nicht** gemeint sind Größen, die aus der Fläche gerechnet
 * sind (`dpSp`): die dürfen nicht ein zweites Mal skaliert werden. Am Emulator ausprobiert
 * und wieder zurückgebaut: der Kopf der Wähltastatur mit 200 % zeigte „Nummer", der Rest
 * lag außerhalb des Bildes, und der Hinweis darunter war ganz verschwunden.
 */
class TextScaleTest {

    /** Die Bildschirme mit Listen - dort ist Platz zum Wachsen, weil sie scrollen. */
    private val bildschirme = Quelltext.dateien().filter { it.name.endsWith("Activity.kt") }

    private val festeGroesse = Regex("""fontSize = \d+(\.\d+)?\.sp""")

    @Test
    fun `auf den Bildschirmen steht keine feste Schriftgroesse`() {
        val fest = mutableListOf<String>()
        bildschirme.forEach { datei ->
            datei.readLines().forEachIndexed { index, zeile ->
                if (festeGroesse.containsMatchIn(zeile)) {
                    fest += "${datei.name}:${index + 1}: ${zeile.trim()}"
                }
            }
        }
        assertTrue(
            "Diese Groessen folgen der eingestellten Textgroesse nicht - bigSp() nehmen, " +
                "oder dpSp(), wenn die Groesse aus der Flaeche kommt:\n" + fest.joinToString("\n"),
            fest.isEmpty(),
        )
    }

    @Test
    fun `bigSp gibt es und es liest die Einstellung`() {
        val quelle = Quelltext.datei("org/biglau/ui/TextSizing.kt").readText()
        assertTrue("bigSp fehlt", "fun bigSp(" in quelle)
        assertTrue("bigSp liest die Einstellung nicht", "LocalTextScale.current" in quelle)
    }
}
