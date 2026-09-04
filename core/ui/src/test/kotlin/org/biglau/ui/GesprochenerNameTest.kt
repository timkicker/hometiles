package org.biglau.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein gesprochener Name wirkt auch ohne Zustand.
 *
 * `BigRow` kann einen Namen ersetzen (`labelSpeech`) — für eine Beschriftung, in der etwas
 * Gezeichnetes steckt. Der Semantik-Block hing aber bis zum 04.09.2026 allein an `zustand`:
 * eine Zeile mit ersetztem Namen, aber ohne „ausgewählt", „an", „aus" oder Zustandssatz
 * bekam **gar keine** `contentDescription`, und der übergebene Name fiel lautlos weg.
 *
 * Aufgefallen ist es an den fünf Farbzeilen der Hintergrundauswahl: der Name war übergeben
 * und im Knotenabzug stand nichts. Ein Parameter, der in der Hälfte der Fälle nichts tut,
 * ist schlimmer als keiner — man verlässt sich darauf.
 */
class GesprochenerNameTest {

    private val quelle = File("src/main/kotlin/org/biglau/ui/BigRow.kt").readText()

    @Test
    fun `die ansage haengt nicht allein am zustand`() {
        val weiche = quelle.lines()
            .dropWhile { !it.contains("val ansage = when") }
            .takeWhile { !it.trimStart().startsWith("Row(") }
            .joinToString("\n")
        assertTrue(
            "In BigRow gibt es keine Weiche, die den ersetzten Namen ohne Zustand ansagt:\n" +
                weiche,
            "labelSpeech != null" in weiche,
        )
    }

    @Test
    fun `der semantik-block haengt an der ansage`() {
        assertTrue(
            "Der Semantik-Block prueft weiter `zustand` statt der Ansage - dann bleibt " +
                "labelSpeech ohne Zustand wirkungslos.",
            "if (ansage != null) {" in quelle,
        )
    }
}
