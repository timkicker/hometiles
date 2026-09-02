package org.biglau.design

import java.io.File
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
        "src/main/java/org/biglau/ui/HomeHeader.kt",
        "src/main/java/org/biglau/ui/InfoTiles.kt",
        "src/main/java/org/biglau/phone/InCallActivity.kt",
        "src/main/java/org/biglau/phone/DialerActivity.kt",
    )

    @Test
    fun `jede laufende Zahl bekommt Tabellenziffern`() {
        val ohne = stellen.filterNot { "TabellenZiffern" in File(it).readText() }
        assertTrue("Ohne Tabellenziffern: $ohne", ohne.isEmpty())
    }

    /** Und die Schrift kann es auch - sonst wäre die Angabe wirkungslos. */
    @Test
    fun `die mitgelieferte Schrift kennt tnum`() {
        listOf("atkinson_regular.ttf", "atkinson_bold.ttf").forEach { name ->
            val bytes = File("src/main/res/font/$name").readBytes()
            val marke = "tnum".toByteArray()
            val drin = (0..bytes.size - marke.size).any { i ->
                marke.indices.all { bytes[i + it] == marke[it] }
            }
            assertTrue("$name hat keine Tabellenziffern", drin)
        }
    }
}
