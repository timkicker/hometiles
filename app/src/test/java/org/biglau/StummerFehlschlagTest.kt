package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Tipp darf nicht im Nichts enden.
 *
 * `runCatching` ist bequem: es fängt alles und gibt ein `Result` zurück, das man weglassen
 * darf. An 86 Stellen im Programm steht es, und an den meisten ist das richtig - eine
 * Abfrage, die auch leer sein darf, ein Zuhörer, der ohnehin gleich stirbt.
 *
 * An zwei Stellen war es falsch, denn dort steht ein Mensch mit dem Finger auf dem Glas:
 * die Schalterkacheln und die Knöpfe im Gespräch. Wer dort schweigt, hinterlässt einen
 * Knopf, der nichts tut - und im schlimmsten Fall ist das „Annehmen", während es klingelt.
 */
class StummerFehlschlagTest {

    @Test
    fun `in ToggleActions endet kein Versuch stumm`() {
        val zeilen = Quelltext.file("org/biglau/toggles/ToggleActions.kt").readLines()
        val ohneAusweg = zeilen.withIndex()
            .filter { (_, zeile) -> "runCatching" in zeile && !Quelltext.isCommentLine(zeile) }
            .filter { (i, _) ->
                // Der Ausweg darf im selben Ausdruck stehen - eine Zeile weiter oder bis
                // zur schliessenden Klammer des Blocks.
                zeilen.drop(i).take(20).takeWhile { "runCatching" !in it || it == zeilen[i] }
                    .none { "onFailure" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "Hier endet ein Schalter-Tipp ohne Ausweg. ToggleActions ist der letzte Halt: " +
                "danach kommt kein Rückfall mehr, nur noch eine Kachel, die nichts tut.",
            emptyList<Int>(),
            ohneAusweg,
        )
    }

    @Test
    fun `kein Ergebnis von InCallRepository wird weggeworfen`() {
        // Welche Funktionen ein `Result` liefern, steht in der Quelle - nicht in einer
        // Liste hier, die altert. `attach`, `detach` und `publish` sind Buchhaltung und
        // liefern nichts; sie duerfen als eigene Anweisung stehen.
        val mitResult = Quelltext.file("org/biglau/phone/InCallRepository.kt")
            .readLines()
            .mapNotNull { zeile ->
                Regex("""fun (\w+)\([^)]*\)[^=]*= runCatching""").find(zeile)?.groupValues?.get(1)
            }
            .toSet()
        assertTrue("Keine Result-Funktion gefunden - liest die Regel noch, was sie meint?", mitResult.size >= 5)

        val weggeworfen = Quelltext.files()
            .filter { it.name != "InCallRepository.kt" }
            .flatMap { datei ->
                datei.readLines().withIndex()
                    .filter { (_, zeile) ->
                        val nackt = zeile.trim()
                        mitResult.any { nackt.startsWith("InCallRepository.$it(") }
                    }
                    .map { (i, _) -> "${datei.name}:${i + 1}" }
            }
        assertEquals(
            "Ein Aufruf an InCallRepository als eigene Anweisung wirft sein Result weg. " +
                "Jede dieser Anweisungen kann fehlschlagen, weil der Anruf inzwischen weg " +
                "ist - dann muss es der Mensch davor erfahren.",
            emptyList<String>(),
            weggeworfen,
        )
    }

    @Test
    fun `der Anrufbildschirm meldet einen Fehlschlag`() {
        val quelle = Quelltext.file("org/biglau/phone/InCallActivity.kt").readText()
        assertTrue(
            "InCallActivity prüft den Ausgang nicht mehr",
            "isFailure" in quelle && "R.string.call_action_failed" in quelle,
        )
    }
}
