package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Was kein Android braucht, gehört nicht nach `:app`.
 *
 * Am 3.9.2026 lagen 22 Dateien - rund 1300 Zeilen - im Programmodul, die keinen einzigen
 * Import ausserhalb von `org.biglau` hatten: Sortierungen, Suche, Filter, Zustands-
 * rechnungen. Sie lagen dort nicht aus einem Grund, sondern weil sie dort entstanden sind.
 *
 * Das ist kein Schönheitsfehler. Solche Dateien halten den Modulschnitt aus `PLAN.md` 2.1
 * auf, sie machen die Testläufe von `:app` länger als nötig, und sie können sich unbemerkt
 * an Android binden, weil in `:app` nichts sie daran hindert. In `core:model` verbietet es
 * der Übersetzer.
 *
 * Deshalb: eine neue Datei ohne Android gehört in ein Kernmodul. Gibt es einen Grund,
 * warum sie doch hierbleiben muss, steht er unten - mit Namen.
 */
class OhneAndroidTest {

    /** Datei → warum sie trotzdem in `:app` bleibt. */
    private val darfBleiben = mapOf<String, String>()

    @Test
    fun `keine Datei ohne Android bleibt in app`() {
        // Nur was **Android** ist. `java.*` und `kotlin.*` gibt es in `core:model` genauso -
        // die erste Fassung dieser Regel zaehlte sie mit und uebersah dadurch `MessageStamps`,
        // 31 Zeilen Datumsrechnung, die nur `java.util` braucht.
        val fremd = listOf("android", "androidx", "coil")
        val rein = Quelltext.appRoot.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { datei ->
                datei.readLines()
                    .filter { it.startsWith("import ") }
                    .none { zeile -> fremd.any { zeile.removePrefix("import ").startsWith(it) } }
            }
            .map { it.name }
            .toSortedSet()

        assertEquals(
            "Diese Datei braucht kein Android und gehört damit in ein Kernmodul - Paket " +
                "behalten, Modul wechseln, dann ändert sich kein einziger Import. Muss sie " +
                "doch bleiben, mit Grund in die Liste in OhneAndroidTest.",
            darfBleiben.keys.toSortedSet(),
            rein,
        )
    }
}
