package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Eine Regel, die im Quelltext schneidet, faellt um, wenn die Marke fehlt - statt gruen zu
 * bleiben.
 *
 * `substringAfter("X")` gibt bei fehlendem `X` **den ganzen Text** zurueck, `substringBefore`
 * auch. Eine Regel schneidet dann nicht mehr den Abschnitt heraus, den sie meint, sondern
 * behaelt alles - und findet ihr Stichwort irgendwo anders. In der Nacht auf den 04.09.2026
 * ist das zweimal passiert: `AuswahlAnsageTest` nahm den Farbtonwaehler mit (die Endmarke
 * lag hinter dem Abschnitt), und `FremdeAbsichtTest` haette bei einer umbenannten Variablen
 * den ganzen Rest der Datei durchsucht. Beide waren gruen und prueften nichts.
 *
 * `Quelltext.ausschnitt` wirft in diesem Fall. Achtzig Schnittstellen sind darauf umgestellt;
 * diese Regel haelt die letzten drei fest, bei denen die alte Form richtig ist.
 */
class LauteSchnitteTest {

    /**
     * Wo `substringAfter`/`substringBefore` bleiben darf, und warum.
     *
     * `DeadTileTest` gibt den Ersatzwert `""` mit - fehlt die Marke, kommt nichts zurueck
     * statt alles, und die Regel faellt von selbst um. `LupentasteTest` schneidet eine
     * Zeile, von der es vorher geprueft hat, dass sie die Marke enthaelt.
     * `AddressFormTest` schneidet keinen Quelltext, sondern einen Satz: dort ist "es gibt
     * kein Leerzeichen, nimm das ganze Wort" genau richtig.
     */
    private val erlaubt = mapOf(
        "DeadTileTest.kt" to "gibt einen Ersatzwert mit",
        "LupentasteTest.kt" to "prueft die Marke vorher",
        "AddressFormTest.kt" to "schneidet einen Satz, keinen Quelltext",
    )

    @Test
    fun `Regeln schneiden mit einer Marke, die es geben muss`() {
        val stumm = Quelltext.testDateien()
            .filterNot { it.name in erlaubt }
            .flatMap { datei ->
                datei.readLines().mapIndexedNotNull { nummer, zeile ->
                    val text = zeile.trim()
                    if (text.startsWith("//") || text.startsWith("*")) {
                        null
                    } else if (Regex("""\.substring(After|Before)\(""").containsMatchIn(zeile)) {
                        "${datei.name}:${nummer + 1}: $text"
                    } else {
                        null
                    }
                }
            }
        assertEquals(
            "Hier wird an einer Marke geschnitten, die es eines Tages nicht mehr gibt - " +
                "dann liefert der Schnitt den ganzen Text und die Regel bleibt gruen, ohne " +
                "noch etwas zu pruefen. Quelltext.ausschnitt nehmen; die faellt dann um:\n" +
                stumm.joinToString("\n"),
            emptyList<String>(),
            stumm,
        )
    }

    /**
     * Und ein Text wird in **allen** Modulen gesucht, nicht nur im ersten.
     *
     * `Quelltext.texte(sprache).first()` ist `:app` und sonst nichts. In dieser Nacht sind
     * Texte dreimal in ein anderes Modul gezogen - `a11y_chosen` nach `core:ui`, die sechs
     * Woerter fuer die Richtung eines Anrufs nach `core:system`. Fuenf Regeln haetten danach
     * ins Leere gegriffen. `Quelltext.textWert` sieht ueberall nach und faellt um, wenn es
     * den Text nirgends gibt.
     */
    @Test
    fun `ein Text wird in allen Modulen gesucht`() {
        val nurAppp = Quelltext.testDateien().flatMap { datei ->
            datei.readLines().mapIndexedNotNull { nummer, zeile ->
                // Kommentare zaehlen nicht: zum dritten Mal in dieser Nacht hat eine
                // Regel ihre eigene Begruendung als Verstoss gemeldet.
                val text = zeile.trim()
                if (text.startsWith("//") || text.startsWith("*")) {
                    null
                } else if (Regex("""texte\([^)]*\)\s*\.first\(\)""").containsMatchIn(zeile)) {
                    "${datei.name}:${nummer + 1}: ${zeile.trim()}"
                } else {
                    null
                }
            }
        }
        assertEquals(
            "Hier wird ein Text nur in :app gesucht. Zieht er in ein anderes Modul, greift " +
                "die Regel ins Leere. Quelltext.textWert nehmen:\n" + nurAppp.joinToString("\n"),
            emptyList<String>(),
            nurAppp,
        )
    }
}
