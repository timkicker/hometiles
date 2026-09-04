package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was das Verschieben an der Kachel aendert, steht vorher da.
 *
 * `TileMove.move` setzt eine grosse Kachel auf ein Feld zurueck, sonst ragte sie am Ziel
 * ueber den Rand - das ist richtig so und seit langem geprueft. Ungeprueft war, dass es
 * jemand erfaehrt: die Verschieben-Ansicht zeigte "Screen 2 - 5 Plaetze frei", man tippte,
 * und die 2x1-Kachel stand drueben als Quadrat. Hinterher sieht das nach einem Fehler aus,
 * vorher ist es eine Bedingung.
 *
 * Die Regel haengt an der Stelle im Modell, die schrumpft. Faellt das Schrumpfen weg, faellt
 * auch der Hinweis weg duerfen - dann faellt hier der erste Satz um und sagt das.
 */
class VerkleinertTest {

    private val modell = Quelltext.ohneKommentare("org/biglau/tiles/TileMove.kt")
    private val editor = Quelltext.ohneKommentare("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `das Modell verkleinert die Kachel beim Umzug immer noch`() {
        val umzug = Quelltext.ausschnitt(modell, "fun move(")
        assertTrue(
            "TileMove.move schrumpft nicht mehr - dann darf der Hinweis in der " +
                "Verschieben-Ansicht weg, und diese Regel auch.",
            umzug.contains("w = 1") && umzug.contains("h = 1"),
        )
    }

    @Test
    fun `die Verschieben-Ansicht sagt es, bevor es passiert`() {
        val ansicht = Quelltext.ausschnitt(editor, "fun MoveTargetList(")
        assertTrue(
            "Die Verschieben-Ansicht nennt das Schrumpfen nicht. Eine Kachel, die beim " +
                "Verschieben still kleiner wird, sieht aus wie ein Fehler.",
            ansicht.contains("move_shrinks"),
        )
    }

    @Test
    fun `der Hinweis kommt nur bei einer grossen Kachel`() {
        val bedingung = Quelltext.ausschnitt(editor, "shrinks = ", "\n")
        assertTrue(
            "Die Bedingung fuer den Hinweis liest nicht die Groesse der Kachel, sondern " +
                "steht auf $bedingung - dann stuende er auch bei einer Kachel, die gar " +
                "nicht schrumpfen kann.",
            bedingung.contains(".w >") && bedingung.contains(".h >"),
        )
    }

    @Test
    fun `der Hinweis steht in beiden Sprachen und weist einen Weg`() {
        for (datei in Quelltext.texte("values") + Quelltext.texte("values-de")) {
            val text = datei.readText()
            if (!text.contains("name=\"move_shrinks\"")) continue
            val satz = Quelltext.ausschnitt(text, "name=\"move_shrinks\">", "</string>")
            assertTrue(
                "Der Hinweis in ${datei.path} nennt nur das Problem: $satz",
                satz.count { it == '.' } >= 2,
            )
        }
        val gefunden = (Quelltext.texte("values") + Quelltext.texte("values-de"))
            .count { it.readText().contains("name=\"move_shrinks\"") }
        assertTrue("Der Hinweis fehlt in einer der beiden Sprachen.", gefunden == 2)
    }
}
