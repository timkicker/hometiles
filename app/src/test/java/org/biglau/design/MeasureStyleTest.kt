package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gemessen wird mit dem Stil, der auch gezeichnet wird.
 *
 * Diese App misst Text, bevor sie ihn zeichnet - Kachelbeschriftungen, Zeilen, das Datum
 * auf der Uhr-Kachel. Gemessen wurde dabei an drei Stellen mit einem **frisch gebauten**
 * `TextStyle`, in dem nur die Groesse stand. Der Nutzer hat aber „Hyperlegible" gewaehlt,
 * und die ist breiter als die Standardschrift: die Messung sagte „passt in eine Zeile", und
 * auf dem Geraet brach es um. So gesehen an der Uhr-Kachel, wo unter „Wednesday, September"
 * die **2 allein** stand.
 *
 * Der Fehler ist leise, weil er nur bei fremder Schrift auftritt - wer mit der Systemschrift
 * entwickelt, sieht ihn nie. Deshalb eine Regel und kein Merksatz.
 */
class MeasureStyleTest {

    private fun messende() = Quelltext.dateien().filter { "rememberTextMeasurer" in it.readText() }

    @Test
    fun `wer misst kennt den gezeichneten Stil`() {
        val ohne = messende().filterNot { "LocalTextStyle.current" in it.readText() }
        assertEquals("misst ohne den Stil der Oberflaeche: $ohne", emptyList<Any>(), ohne)
    }

    /**
     * Kein frisch gebautes `TextStyle` in einer messenden Datei. Erlaubt ist `copy` vom
     * Stil der Oberflaeche - dann bleibt die Schrift des Nutzers erhalten.
     */
    @Test
    fun `kein frisch gebauter Stil in einer messenden Datei`() {
        val muster = Regex("""(?:=|to|style =)\s*TextStyle\(""")
        val treffer = messende().flatMap { datei ->
            datei.readLines().withIndex()
                .filter { muster.containsMatchIn(it.value) }
                .map { "${datei.name}:${it.index + 1}  ${it.value.trim()}" }
        }
        assertEquals("frisch gebauter Stil statt copy: $treffer", emptyList<String>(), treffer)
    }

    /** Und es gibt sie wirklich - sonst pruefte die Regel eine leere Menge. */
    @Test
    fun `es gibt messende Dateien`() {
        assertTrue("keine Datei misst mehr Text", messende().size >= 3)
    }

    /**
     * Kein eigenstaendiger `TextStyle` als Vorrat.
     *
     * `Text(style = …)` **ersetzt** den Stil der Umgebung, es ergaenzt ihn nicht. Ein
     * `val TabellenZiffern = TextStyle(fontFeatureSettings = "tnum")` warf damit die
     * eingestellte Schrift weg: Uhr, Ladestand, Waehltastatur und Gespraechsdauer standen
     * in der Systemschrift, alles daneben in der des Nutzers. Ein Schriftwechsel mitten auf
     * dem Bildschirm liest sich wie ein Fehler - und war hier einer.
     */
    @Test
    fun `kein Stil auf Vorrat neben dem der Oberflaeche`() {
        val muster = Regex("""^(?:internal |private )?val \w+\s*(?::\s*TextStyle\s*)?= TextStyle\(""")
        val treffer = Quelltext.dateien().flatMap { datei ->
            datei.readLines().withIndex()
                .filter { muster.containsMatchIn(it.value) }
                .map { "${datei.name}:${it.index + 1}  ${it.value.trim()}" }
        }
        assertEquals("Stil ohne die Schrift der Oberflaeche: $treffer", emptyList<String>(), treffer)
    }
}
