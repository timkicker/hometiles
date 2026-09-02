package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logik, die nur der Test aufruft, ist keine Logik der App.
 *
 * Anlass: ein Durchlauf über alle `object`-Funktionen fand **22 Stück**, die keine Stelle
 * der App aufrief — jede davon mit Tests, alle grün. Darunter `FolderEdits.orphaned`, auf
 * das ein Kommentar ausdrücklich verwies („ob eine fehlt, prüft FolderEdits.orphaned"),
 * und `CellLayout.fitToGrid`, dessen Rechnung ein zweites Mal von Hand in `setGrid` stand —
 * die getestete Fassung lief nie, die laufende war ungetestet.
 *
 * Das ist die teuerste Sorte Fehler in diesem Projekt: die Tests sagen, es funktioniere,
 * und sie haben recht — nur benutzt es niemand. Geprüft werden Funktionen in `object`s,
 * weil das die reine Logik ist; Klassen und Composables haben Aufrufwege, die kein
 * Textvergleich sicher findet.
 */
class DeadLogicTest {


    /**
     * Was es geben darf, ohne dass die App es aufruft.
     *
     * Nur mit Grund und nur mit Pfad zur Wiedervorlage — eine Ausnahme ohne beides ist
     * bloss ein leiser gestellter Fehler.
     */
    private val begruendeteAusnahmen = emptyMap<String, String>()

    private fun dateien(): List<File> =
        Quelltext.dateien()

    /** Funktionsname → "Objekt.Name", für alle Funktionen direkt in einem `object`. */
    private fun deklarationen(): List<Pair<String, String>> {
        val gefunden = mutableListOf<Pair<String, String>>()
        dateien().forEach { datei ->
            var objekt: String? = null
            datei.readLines().forEach { zeile ->
                Regex("""^\s*(?:internal\s+)?object\s+(\w+)""").find(zeile)?.let {
                    objekt = it.groupValues[1]
                }
                Regex("""^\s{4}(?:internal\s+)?fun\s+(?:<[^>]*>\s*)?(\w+)\s*\(""").find(zeile)?.let {
                    val name = it.groupValues[1]
                    objekt?.let { o -> gefunden += name to "$o.$name" }
                }
            }
        }
        return gefunden
    }

    private val quelltext: String by lazy { dateien().joinToString("\n") { it.readText() } }

    /** Aufrufe minus Deklarationen, plus Methodenverweise (`::name`). */
    private fun wirdAufgerufen(name: String): Boolean {
        val aufrufe = Regex("""\b${Regex.escape(name)}\s*\(""").findAll(quelltext).count()
        val deklarationen =
            Regex("""fun\s+(?:<[^>]*>\s*)?${Regex.escape(name)}\s*\(""").findAll(quelltext).count()
        val verweise = Regex("""::${Regex.escape(name)}\b""").findAll(quelltext).count()
        return aufrufe - deklarationen + verweise > 0
    }

    @Test
    fun `jede Funktion in einem object wird auch aufgerufen`() {
        val tot = deklarationen()
            .filterNot { (name, _) -> wirdAufgerufen(name) }
            .map { it.second }
            .filterNot { it in begruendeteAusnahmen }
            .distinct()
            .sorted()
        assertEquals(
            "Diese Logik ruft nur der Test auf. Entweder fehlt die Stelle, an der sie " +
                "laufen sollte, oder es gibt sie schon woanders und das hier ist die " +
                "zweite Kopie: $tot",
            emptyList<String>(),
            tot,
        )
    }

    @Test
    fun `jede Ausnahme nennt ihren Grund und gibt es wirklich`() {
        val bekannt = deklarationen().map { it.second }.toSet()
        begruendeteAusnahmen.forEach { (eintrag, grund) ->
            assertTrue("$eintrag gibt es nicht mehr - Ausnahme streichen", eintrag in bekannt)
            assertTrue("$eintrag braucht einen Grund", grund.length > 20)
        }
    }

    @Test
    fun `die Regel findet einen erfundenen Namen nicht`() {
        // Gegenprobe: ohne sie wuerde ein kaputter Suchausdruck alles durchwinken.
        assertTrue(!wirdAufgerufen("dieseFunktionGibtEsNicht"))
    }

    @Test
    fun `die Regel erkennt einen echten Aufruf`() {
        assertTrue("group wird von CallLogRepository aufgerufen", wirdAufgerufen("group"))
    }
}
