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

    private val start = Quelltext.withoutComments("org/biglau/MainActivity.kt")

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
        val bedingung = Quelltext.cut(start, "val verdeckt = ", "Column(")
        // Die Ueberlagerungen: was nach dem Raster des Startbildschirms noch bedingt
        // gezeigt wird. `then(wischen)` gehoert zum Startbildschirm und nur zu ihm - es ist
        // seine Wischgeste zwischen den Screens. Vorher stand hier `zeigeKachel(screen`, und
        // das ging am 04.09.2026 kaputt, als der Aufruf zwei Zeilen bekam: die Marke ist die
        // Formatierung mitgemeint. Ein Ausdruck, der nur an dieser einen Stelle vorkommen
        // **kann**, haelt laenger.
        val danach = Quelltext.cut(start, "then(wischen)", "private fun")
        // Zwei Schreibweisen, nicht eine. Bis zum 04.09.2026 stand hier nur die erste, und
        // genau daran ist die Regel vorbeigelaufen: die Liste der Menuetaste kam als
        // `kachelMenue.value?.let { ... }` dazu, wurde nicht gezaehlt und fehlte in der
        // Bedingung. Bei offener Liste standen `Nachrichten`, `Telefon` und `Kontakte`
        // weiter im Knotenabzug. Eine Regel, die nur eine von zwei Formen kennt, bleibt
        // gruen und prueft die Haelfte.
        val formen = listOf(
            Regex("""\n {16}if \((\w+(?:\.\w+)*) != null\) \{"""),
            Regex("""\n {16}(\w+(?:\.\w+)*)\?\.let \{"""),
        )
        val fehlend = formen
            .flatMap { form -> form.findAll(danach).map { it.groupValues[1] } }
            .map { it.removeSuffix(".value") }
            .filterNot { it in bedingung }
            .distinct()
            .toList()
        assertEquals(
            "Diese Ueberlagerungen fehlen in der Bedingung - solange sie offen sind, " +
                "steht der Startbildschirm darunter weiter in der Vorlesefunktion: $fehlend",
            emptyList<String>(),
            fehlend,
        )
    }
}
