package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine gemessene Tatsache ueber das Geraet traegt ihr Datum.
 *
 * Der Quelltext ist voll von Saetzen wie „auf dem Telefon des Nutzers steht
 * `WRITE_CALL_LOG` auf granted=false". Solche Saetze sind wertvoll: sie erklaeren, warum
 * ein Umweg da ist, und sie sind nachgemessen statt vermutet. Sie haben nur eine
 * Eigenschaft, die man ihnen nicht ansieht - **sie altern**.
 *
 * In der Nacht auf den 04.09.2026 hat BigLau die Telefon- und die SMS-Rolle bekommen.
 * Android erteilt mit diesen Rollen mehrere Rechte mit, darunter `WRITE_CALL_LOG`. Damit
 * waren an einem Abend **sechs** Kommentare falsch, jeder einzeln richtig aufgeschrieben
 * und keiner mehr wahr. Gefunden habe ich sie nur, weil ich zufaellig an einer davon
 * vorbeikam.
 *
 * Der Grund fuer den Umweg bleibt in jedem der sechs Faelle gueltig - ein Weg, der an
 * keiner Rolle haengt, ist der bessere. Falsch war nicht die Entscheidung, sondern die
 * Zeitform: Gegenwart fuer etwas, das gemessen wurde.
 *
 * Also: wer das Geraet des Nutzers als Beleg anfuehrt, schreibt dazu, wann. Dann liest man
 * spaeter einen Messwert und keine Behauptung.
 */
class GemesseneTatsacheTest {

    // Auch einstellige Tage und Monate: im Quelltext steht sowohl 3.9.2026 als auch
    // 03.09.2026. Die erste Fassung verlangte zwei Ziffern und meldete drei Stellen, die
    // ihr Datum laengst trugen.
    private val datum = Regex("""\d{1,2}\.\d{1,2}\.\d{4}""")

    /** Saetze, die sich auf das eine Geraet berufen. */
    private val beruft = listOf("Telefon des Nutzers", "Gerät des Nutzers", "Geraet des Nutzers")

    @Test
    fun `wer das Geraet als Beleg anfuehrt, nennt das Datum`() {
        val ohneDatum = (Quelltext.files() + Quelltext.testFiles())
            // Die Regel selbst redet ueber solche Saetze, statt welche zu behaupten.
            .filterNot { it.name == "GemesseneTatsacheTest.kt" }
            .flatMap { datei ->
                val zeilen = datei.readLines()
                zeilen.withIndex()
                    .filter { (_, z) -> beruft.any { it in z } }
                    .filterNot { (i, _) ->
                        // Das Datum darf im selben Absatz stehen, nicht nur in derselben
                        // Zeile - ein Absatz bricht spaetestens nach fuenf Zeilen um.
                        zeilen.subList(maxOf(0, i - 4), minOf(zeilen.size, i + 5))
                            .any { datum.containsMatchIn(it) }
                    }
                    .map { (i, z) -> "${datei.name}:${i + 1}: ${z.trim().take(80)}" }
            }
        assertEquals(
            "Hier steht eine Tatsache ueber das Geraet des Nutzers ohne Datum. Solche " +
                "Saetze altern still: am 04.09.2026 waren sechs davon auf einmal falsch, " +
                "weil BigLau zwei Rollen bekommen hat.",
            emptyList<String>(),
            ohneDatum,
        )
    }

    /** Und die Regel findet ueberhaupt etwas - sonst prueft sie nichts. */
    @Test
    fun `es gibt solche Saetze wirklich`() {
        val treffer = (Quelltext.files() + Quelltext.testFiles())
            .count { datei -> beruft.any { it in datei.readText() } }
        assertTrue(
            "Keine einzige Stelle beruft sich mehr auf das Geraet - liest die Regel noch, " +
                "was sie meint?",
            treffer >= 5,
        )
    }
}
