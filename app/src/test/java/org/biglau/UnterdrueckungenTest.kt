package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Jede `@Suppress`-Zeile sagt, warum sie dasteht.
 *
 * Eine Unterdrückung schaltet eine Warnung ab, die jemand einmal für wichtig hielt. Wer sie
 * ohne ein Wort setzt, hinterlässt eine Entscheidung, die niemand nachprüfen kann - und in
 * zwei Jahren traut sich keiner mehr, sie wegzunehmen. Am 3.9.2026 standen zehn solcher
 * Zeilen im Programm, keine davon mit Grund.
 *
 * `"unused"` ist gar nicht erlaubt: was niemand ruft, gehört weg und nicht zum Schweigen
 * gebracht. Genau so eine Funktion fand sich in `ShortcutRepository`.
 */
class UnterdrueckungenTest {

    private fun stellen(): List<Triple<String, Int, String>> =
        Quelltext.dateien().flatMap { datei ->
            val zeilen = datei.readLines()
            zeilen.withIndex()
                .filter { (_, zeile) -> zeile.trimStart().startsWith("@Suppress") }
                .map { (i, zeile) -> Triple(datei.name, i + 1, zeile.trim()) }
                .map { (name, nr, zeile) ->
                    Triple("$name:$nr", nr, zeilen.getOrElse(nr - 2) { "" }.trim() + "|" + zeile)
                }
        }

    @Test
    fun `jede unterdrueckung nennt ihren grund`() {
        val ohne = stellen()
            .filter { (_, _, umfeld) ->
                val davor = umfeld.substringBefore("|")
                !(davor.startsWith("//") || davor.startsWith("*") || davor.startsWith("/*"))
            }
            .map { it.first }
        assertEquals(
            "Eine @Suppress-Zeile ohne Kommentar darüber. Schreib hin, welche Warnung " +
                "warum abgeschaltet wird - sonst nimmt sie später niemand mehr weg.",
            emptyList<String>(),
            ohne,
        )
    }

    @Test
    fun `nichts wird als unbenutzt totgeschwiegen`() {
        val stumm = stellen()
            .filter { (_, _, umfeld) -> "\"unused\"" in umfeld.substringAfter("|") }
            .map { it.first }
        assertEquals(
            "Was niemand ruft, wird gelöscht und nicht mit @Suppress(\"unused\") ruhiggestellt.",
            emptyList<String>(),
            stumm,
        )
    }
}
