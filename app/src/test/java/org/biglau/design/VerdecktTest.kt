package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was verdeckt ist, gibt es auch fuer die Vorlesefunktion nicht.
 *
 * Der Startbildschirm und seine Ueberlagerungen - ein offener Ordner, die grosse
 * Kachelbeschriftung, die PIN-Sperre, die Kontaktwahl, der Erklaerbildschirm zum Empfang -
 * liegen als Geschwister uebereinander. Der Ordner deckt alles ab, aber die Kacheln
 * darunter blieben in der Bedienungshilfen-Sicht stehen: am 04.09.2026 im Knotenabzug
 * nachgemessen, auch im komprimierten - fuenfzig Knoten mit WhatsApp, Maps, HSL, Spotify,
 * Apps und AnkiDroid darin. Danach neunundzwanzig, nur noch der Ordner selbst.
 *
 * Wer sich vorlesen laesst, wanderte also durch Kacheln, die er nicht sieht, und startete
 * mit einem Doppeltipp eine App, die gar nicht dasteht.
 */
class VerdecktTest {

    private val start = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    @Test
    fun `der Startbildschirm verschwindet, solange etwas darueber liegt`() {
        assertTrue(
            "Der Startbildschirm wird nicht aus der Bedienungshilfen-Sicht genommen, " +
                "solange eine Ueberlagerung offen ist.",
            "clearAndSetSemantics {}" in start,
        )
    }

    /**
     * Und **jede** Ueberlagerung zaehlt mit.
     *
     * Der Fehler kommt zurueck, sobald jemand eine neue Ueberlagerung dazulegt und die
     * Bedingung vergisst. Deshalb wird hier nachgezaehlt: alles, was nach dem
     * Startbildschirm noch bedingt gezeigt wird, muss in der Bedingung stehen.
     */
    @Test
    fun `jede Ueberlagerung steht in der Bedingung`() {
        val bedingung = Quelltext.ausschnitt(start, "val verdeckt = ", "Column(")
        // Die Ueberlagerungen: was nach dem Raster des Startbildschirms noch bedingt
        // gezeigt wird. `zeigeKachel(screen` ist die letzte Zeile des Startbildschirms.
        val danach = Quelltext.ausschnitt(start, "zeigeKachel(screen", "private fun")
        val fehlend = Regex("""\n {16}if \((\w+(?:\.\w+)*) != null\) \{""")
            .findAll(danach)
            .map { it.groupValues[1] }
            .filterNot { it in bedingung }
            .toList()
        assertEquals(
            "Diese Ueberlagerungen fehlen in der Bedingung - solange sie offen sind, " +
                "steht der Startbildschirm darunter weiter in der Vorlesefunktion: $fehlend",
            emptyList<String>(),
            fehlend,
        )
    }
}
