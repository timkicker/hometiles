package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zahlen, die sich ändern, stehen in Tabellenziffern.
 *
 * `PLAN.md` 3.7: „Zahlen (Anrufliste, Wähltastatur, Dauer) mit Tabellenziffern […], damit
 * Spalten nicht springen." Das war nirgends umgesetzt — obwohl die mitgelieferte Schrift
 * `tnum` kann; sie wurde nur nie danach gefragt.
 *
 * Gemessen am Bildschirm, vorher: „11 %" in der Kopfzeile war 64 Pixel breit, „88 %" 74.
 * Die Anzeige rutschte bei jedem Prozent hin und her. Nachher: 57 gegen 59.
 */
class TabularDigitsTest {

    /** Stellen, an denen sich eine Zahl an Ort und Stelle ändert. */
    private val stellen = listOf(
        "org/biglau/ui/HomeHeader.kt",
        "org/biglau/ui/InfoTiles.kt",
        "org/biglau/phone/InCallActivity.kt",
        "org/biglau/phone/DialerActivity.kt",
    )

    @Test
    fun `jede laufende Zahl bekommt Tabellenziffern`() {
        val ohne = stellen.filterNot { "tabularFigures()" in Quelltext.datei(it).readText() }
        assertTrue("Ohne Tabellenziffern: $ohne", ohne.isEmpty())
    }

    /**
     * Und sie kommen vom Stil der Oberfläche, nicht aus einem eigenen `TextStyle`.
     *
     * `Text(style = …)` ersetzt den Stil der Umgebung. Ein Vorrat-Stil, der nur `tnum`
     * setzt, warf damit die eingestellte Schrift weg — die Zahlen standen in der
     * Systemschrift, während alles daneben in der des Nutzers stand. Ausgerechnet die
     * Schrift, von der der zweite Test hier sagt, dass sie `tnum` kann.
     */
    @Test
    fun `die Tabellenziffern behalten die Schrift der Oberflaeche`() {
        val quelle = Quelltext.datei("org/biglau/ui/TextSizing.kt").readText()
        assertTrue("tabularFigures baut einen eigenen Stil", "LocalTextStyle.current.copy(" in quelle)
        assertTrue("es gibt wieder einen Vorrat-Stil", "val TabellenZiffern" !in quelle)
    }

    /** Und die Schrift kann es auch - sonst wäre die Angabe wirkungslos. */
    @Test
    fun `die mitgelieferte Schrift kennt tnum`() {
        listOf("atkinson_regular.ttf", "atkinson_bold.ttf").forEach { name ->
            val bytes = Quelltext.ressource("font/$name").readBytes()
            val marke = "tnum".toByteArray()
            val drin = (0..bytes.size - marke.size).any { i ->
                marke.indices.all { bytes[i + it] == marke[it] }
            }
            assertTrue("$name hat keine Tabellenziffern", drin)
        }
    }
}
