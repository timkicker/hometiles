package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zwei leere Plätze sind zwei Plätze, nicht zweimal derselbe.
 *
 * Jede leere Kachel lädt mit demselben Wort ein: „Antippen". Für das Auge ist das richtig —
 * man sieht ja, wo sie liegt. Am 04.09.2026 mit `tools/gleiche-namen.py` auf dem
 * Startbildschirm gefunden: zwei anklickbare Flächen, ein Name, kein Unterschied für
 * jemanden, der sie nicht sieht.
 *
 * Und die Kette schweigt weiter: der Kachel-Editor sagt danach „Belegt mit: Leer" und nennt
 * die Zelle auch nicht. Wer eine von zweien belegt, weiss also an keiner Stelle, welche.
 *
 * Gesagt wird jetzt der Platz — mit denselben Worten wie die Verschieben-Ansicht, damit
 * beide Bildschirme dasselbe Raster beschreiben.
 */
class LeerePlaetzeTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/ui/HomeScreenView.kt")

    @Test
    fun `eine leere kachel sagt ihren platz`() {
        val leer = Quelltext.ausschnitt(
            quelle,
            von = "private fun EmptyTile(",
            bis = "\n}",
        )
        assertTrue(
            "Die leere Kachel nennt ihren Platz nicht - dann heissen alle leeren Kacheln " +
                "gleich:\n$leer",
            "move_spot" in leer,
        )
    }

    @Test
    fun `beide wege zur leeren kachel geben den platz mit`() {
        // Es gibt zwei: ein Rasterplatz ohne Zelle und eine Zelle ohne Aktion. Gaebe nur
        // einer den Platz mit, waere die Haelfte der leeren Kacheln weiter stumm - und
        // welche, haenge davon ab, wie die Konfiguration zufaellig aussieht.
        val aufrufe = Regex("EmptyTile\\(").findAll(quelle).count()
        val mitPlatz = Regex("column = ").findAll(quelle).count()
        assertTrue(
            "EmptyTile wird ${aufrufe - 1}-mal aufgerufen, aber nur $mitPlatz-mal mit Platz",
            mitPlatz >= aufrufe - 1,
        )
    }

    @Test
    fun `die worte fuer den platz gibt es in beiden sprachen`() {
        listOf("values-de", "values").forEach { sprache ->
            val text = Quelltext.textWert("move_spot", sprache)
            assertTrue(
                "move_spot braucht Zeile und Spalte (Sprache \"$sprache\"): $text",
                "%1\$d" in text && "%2\$d" in text,
            )
        }
    }
}
