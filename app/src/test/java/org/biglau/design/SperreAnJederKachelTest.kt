package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Kachel startet nichts, ohne die Sperre gefragt zu haben.
 *
 * Die App-Kachel fragte, die Verknuepfungs-Kachel nicht. Wer eine gesperrte App als
 * **Verknuepfung** auf eine Kachel legte, kam ohne PIN hinein - dieselbe App, dieselbe
 * Sperre, ein anderer Weg. Gefunden am 03.09.2026, drei Zeilen unter der App-Kachel, die
 * es richtig machte.
 *
 * Das ist derselbe Fehler wie bei der Lupentaste in der App-Liste (siehe
 * [LupentasteTest]) und am selben Tag gefunden: die Pruefung haengt am einzelnen Weg
 * statt an der Stelle, an der alle Wege zusammenlaufen. Jetzt gibt es diese Stelle -
 * `starten` - und die Regel haelt fest, dass niemand daran vorbeikommt.
 */
class SperreAnJederKachelTest {

    private val zeilen = Quelltext.file("org/biglau/MainActivity.kt").readLines()

    @Test
    fun `nur eine Stelle startet Apps und Verknuepfungen`() {
        val starts = zeilen.withIndex()
            .filter { (_, z) ->
                val nackt = z.trim()
                !Quelltext.isCommentLine(z) &&
                    ("apps.launch(" in nackt || Regex("""ShortcutRepository[^)]*\)\.launch\(""").containsMatchIn(nackt))
            }
            .map { it.index + 1 }
        assertTrue("Es startet gar nichts mehr - liest die Regel noch, was sie meint?", starts.isNotEmpty())

        val beiStarten = zeilen.indexOfFirst { it.trim().startsWith("private fun starten(") }
        assertTrue("`starten` gibt es nicht mehr", beiStarten > 0)
        val ende = zeilen.drop(beiStarten).indexOfFirst { it == "    }" } + beiStarten + 1

        val draussen = starts.filterNot { it in (beiStarten + 1)..ende }
        assertEquals(
            "Hier wird etwas gestartet, ohne durch `starten` zu gehen. Solange es zwei " +
                "Wege gibt, wird die Sperre an einem davon vergessen - genau so kam die " +
                "Verknuepfungs-Kachel ohne PIN durch.",
            emptyList<Int>(),
            draussen,
        )
    }

    @Test
    fun `vor jedem Start steht die Frage nach der Sperre`() {
        val ohneFrage = zeilen.withIndex()
            .filter { (_, z) -> z.trim().startsWith("starten(") }
            .filter { (i, _) ->
                zeilen.subList(maxOf(0, i - 6), i)
                    .none { "gesperrt(" in it || "onAccept" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "Hier wird `starten` gerufen, ohne dass davor die Sperre gefragt wurde oder " +
                "die PIN schon eingegeben war.",
            emptyList<Int>(),
            ohneFrage,
        )
    }

    @Test
    fun `die Sperre kennt beide Sorten Kachel`() {
        val beiGesperrt = zeilen.indexOfFirst { it.trim().startsWith("private fun gesperrt(") }
        assertTrue("`gesperrt` gibt es nicht mehr", beiGesperrt > 0)
        val rumpf = zeilen.subList(beiGesperrt, minOf(zeilen.size, beiGesperrt + 20)).joinToString("\n")
        assertTrue(
            "Die Sperre fragt nicht nach Verknuepfungen - dann ist sie ueber eine " +
                "Verknuepfungs-Kachel zu umgehen.",
            "ButtonAction.Shortcut" in rumpf && "ButtonAction.App" in rumpf,
        )
    }
}
