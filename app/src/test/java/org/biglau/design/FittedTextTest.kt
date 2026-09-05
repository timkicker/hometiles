package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer nicht umbrechen darf, misst.
 *
 * `softWrap = false` mit `maxLines = 1` und ohne Kürzungszeichen heißt: was nicht passt,
 * wird **stillschweigend abgeschnitten**. Kein Zeichen, keine Ausnahme, kein roter Test —
 * auf dem Telefon des Nutzers (03.09.2026) stand deshalb bei 200 % Textgröße „2:33" statt „2:36 AM".
 *
 * Die Größe kam aus einer Rechnung mit mittlerer Zeichenbreite (0,60 der Schriftgröße). Das
 * ist eine Schätzung, und sie geht bei einer breiteren Schrift nicht auf — dieselbe Lehre,
 * die bei den Kachelbeschriftungen längst als Kommentar danebensteht.
 *
 * Diese Regel hält fest: eine Zeile, die nicht umbrechen darf, bekommt ihre Größe von
 * `fittedSingleLineDp` — das misst mit dem Stil, der auch gezeichnet wird, und wird kleiner,
 * bis es passt. Die Rechnung darf den *Wunsch* liefern, nicht das letzte Wort.
 */
class FittedTextTest {

    /**
     * Zeilen mit `softWrap = false`, samt der Datei und der Zeilennummer.
     *
     * Gesucht wird mit einem Muster, nicht mit der Zeichenkette `"softWrap = false,"`. Die
     * erste Fassung verglich genau darauf — heute passte das auf alle sieben Stellen, aber
     * es hing an einem Komma: der letzte Parameter eines Aufrufs hat keines, und eine
     * Umformatierung hätte die Regel lautlos halbiert. `zaehltAlleStellen` unten rechnet
     * dagegen.
     */
    private val muster = Regex("""softWrap\s*=\s*false""")

    private fun ohneUmbruch(): List<Triple<java.io.File, Int, List<String>>> =
        Quelltext.files().flatMap { datei ->
            val zeilen = datei.readLines()
            zeilen.withIndex()
                .filter { muster.containsMatchIn(it.value) && !it.value.trim().startsWith("*") }
                .map { Triple(datei, it.index, zeilen.subList(maxOf(0, it.index - 20), it.index)) }
        }

    /**
     * Die Regel sieht jede Stelle, an der das Wort vorkommt.
     *
     * Ohne diese Gegenrechnung ist nicht zu merken, wenn das Muster eine Schreibweise nicht
     * mehr trifft: die Regel bleibt grün und prüft weniger.
     */
    @Test
    fun `zaehltAlleStellen`() {
        val roh = Quelltext.files().sumOf { datei ->
            datei.readLines().count {
                "softWrap" in it && !Quelltext.isCommentLine(it)
            }
        }
        assertEquals("Das Muster trifft nicht jede Schreibweise von softWrap", roh, ohneUmbruch().size)
    }

    @Test
    fun `es gibt solche Zeilen ueberhaupt`() {
        assertTrue("keine Zeile mit softWrap = false mehr - Regel ins Leere", ohneUmbruch().size >= 4)
    }

    @Test
    fun `keine unumbrechbare Zeile nimmt die geschaetzte Groesse`() {
        val treffer = ohneUmbruch().filter { (_, _, davor) ->
            davor.any { "fontSize = dpSp(singleLineSizeSp(" in it || "fontSize = dpSp(\n" in it } &&
                davor.none { "fittedSingleLineDp(" in it }
        }.map { (datei, index, _) -> "${datei.name}:${index + 1}" }
        assertEquals("schaetzt statt zu messen: $treffer", emptyList<String>(), treffer)
    }

    /** Und jede von ihnen misst wirklich - sonst genügte es, die Rechnung wegzulassen. */
    @Test
    fun `jede unumbrechbare Zeile misst`() {
        val ohne = ohneUmbruch()
            .filter { (datei, _, _) -> datei.name != "TextSizing.kt" }
            .filterNot { (_, _, davor) -> davor.any { "fittedSingleLineDp(" in it } }
            .map { (datei, index, _) -> "${datei.name}:${index + 1}" }
        assertEquals("misst nicht: $ohne", emptyList<String>(), ohne)
    }
}
