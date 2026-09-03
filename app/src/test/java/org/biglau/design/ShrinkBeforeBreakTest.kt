package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Erst kleiner werden, dann trennen oder kürzen.
 *
 * Bei 200 % Textgröße las man in der App: „Einstellun…" auf der Kachel, „Alles zurücksetze /
 * n" als Überschrift, „Nachrichte / n" in der Liste. Compose trennt ein Wort mitten
 * hindurch, sobald es allein nicht mehr in die Zeile passt — und wer 200 % einstellt, hat
 * mit einem zerrissenen Wort nichts gewonnen.
 *
 * Alle drei Stellen messen jetzt und gehen stufenweise kleiner, bevor sie trennen: die
 * Kachel misst den ganzen Text, Überschrift und Zeile messen das **längste Wort**, denn
 * daran bricht die Zeile.
 */
class ShrinkBeforeBreakTest {

    private fun quelle(pfad: String) = Quelltext.datei(pfad).readText()

    @Test
    fun `Kachel, Ueberschrift und Zeile benutzen dieselbe Leiter`() {
        listOf(
            "org/biglau/ui/BigTile.kt",
            "org/biglau/ui/BigRow.kt",
        ).forEach { pfad ->
            assertTrue("$pfad misst nicht in Stufen", "labelLadder(" in quelle(pfad))
        }
    }

    @Test
    fun `Ueberschrift und Zeile messen das laengste Wort`() {
        val zeilen = quelle("org/biglau/ui/BigRow.kt")
        // Beide stehen in derselben Datei: BigRow und BigHeading.
        assertTrue("das laengste Wort wird nicht gemessen", "longestWord(" in zeilen)
        assertTrue(
            "es wird nur einmal gemessen - eine der beiden Stellen fehlt",
            zeilen.split("longestWord(").size - 1 >= 2,
        )
    }

    /**
     * Drei Zeilen, wo drei Zeilen Platz haben — **gemessen**, nicht an der Art des Aufbaus
     * geraten. Der erste Versuch fragte nur, ob die Höhe überhaupt begrenzt ist; auf dem
     * Notrufbildschirm ist sie das (fester Aufbau), aber reichlich — dort stand deshalb
     * weiter „Kontakte jetzt eintr…", obwohl der halbe Bildschirm leer war.
     */
    @Test
    fun `die dritte Zeile haengt an der gemessenen Hoehe`() {
        val zeilen = quelle("org/biglau/ui/BigRow.kt")
        assertTrue("die Hoehe wird nicht gemessen", "maxLines = 3" in zeilen)
        assertTrue(
            "es wird nicht gegen den verfuegbaren Platz geprueft",
            "constraints.maxHeight" in zeilen,
        )
    }
}
