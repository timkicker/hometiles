package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Erklärung steht bei dem, was sie erklärt.
 *
 * Anlass: an zwölf Stellen standen zwei oder mehr Doku-Blöcke aufeinander, und nur der
 * letzte gehörte zu der Funktion darunter. In `SettingsActivity` waren es vier — die
 * Begründungen für „Ausgeblendete Apps", „Sicherung" und „Notruf" hingen über der
 * Barrierefreiheit-Seite. In `Intents` beschrieben die obersten beiden Blöcke zwei
 * Funktionen, die dreißig Zeilen tiefer standen.
 *
 * In diesem Projekt tragen die Kommentare die Begründung — warum eine Rückfrage zweistufig
 * ist, warum eine Zahl dort steht. Eine Begründung über der falschen Funktion ist schlimmer
 * als keine: sie wird geglaubt. Der Compiler merkt davon nichts, und beim Lesen fällt es
 * nur auf, wenn man beide Stellen kennt.
 */
class DocPlacementTest {

    private val quellen: List<File> = listOf(File("src/main/java"), File("src/test/java"))
        .flatMap { it.walkTopDown().filter { datei -> datei.extension == "kt" } }

    @Test
    fun `kein Doku-Block steht auf einem anderen`() {
        val gestapelt = mutableListOf<String>()
        quellen.forEach { datei ->
            val zeilen = datei.readLines()
            zeilen.forEachIndexed { index, zeile ->
                val naechste = zeilen.getOrNull(index + 1)?.trim() ?: return@forEachIndexed
                if (zeile.trim().endsWith("*/") && naechste.startsWith("/**")) {
                    gestapelt += "${datei.name}:${index + 2}"
                }
            }
        }
        assertEquals(
            "Hier steht eine Erklärung über einer anderen - also über der falschen Sache: " +
                "$gestapelt",
            emptyList<String>(),
            gestapelt,
        )
    }

    @Test
    fun `kein Doku-Block steht am Ende eines Blocks`() {
        // Der andere Fall: die beschriebene Funktion ist weg, die Erklärung blieb stehen.
        val verwaist = mutableListOf<String>()
        quellen.forEach { datei ->
            val zeilen = datei.readLines()
            zeilen.forEachIndexed { index, zeile ->
                val naechste = zeilen.getOrNull(index + 1)?.trim() ?: return@forEachIndexed
                if (zeile.trim().endsWith("*/") && (naechste == "}" || naechste.isEmpty())) {
                    verwaist += "${datei.name}:${index + 2}"
                }
            }
        }
        assertEquals(
            "Diese Erklärung beschreibt nichts mehr: $verwaist",
            emptyList<String>(),
            verwaist,
        )
    }

    @Test
    fun `die Regel findet einen erfundenen Stapel`() {
        // Gegenprobe an einer erfundenen Datei, damit ein kaputter Vergleich auffaellt.
        val zeilen = listOf(" */", "/** zweiter Block */", "fun x() = 1")
        val treffer = zeilen.filterIndexed { index, zeile ->
            zeile.trim().endsWith("*/") && (zeilen.getOrNull(index + 1)?.trim()?.startsWith("/**") == true)
        }
        assertTrue("ein Stapel muss auffallen", treffer.isNotEmpty())
    }
}
