package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Werkzeuge und ihre Beschreibung bleiben beieinander.
 *
 * In `tools/` liegen die Programme fürs Prüfen am Gerät — Dinge, die kein Unit-Test sehen
 * kann. Sie sind über eine einzige Nacht von zwei auf fünf gewachsen, und schon dabei ist
 * eines aus `tools/README.md` herausgefallen: `nachbauen.sh` stand nur im README des
 * Projekts. Ein undokumentiertes Werkzeug benutzt niemand mehr, und ein dokumentiertes, das
 * es nicht gibt, kostet den Leser eine Fehlersuche.
 *
 * Deshalb hier beide Richtungen — und das ausführbare Bit dazu, denn ein Werkzeug, das man
 * erst `chmod`en muss, ist eine Stolperstelle mehr auf dem Weg zu einer Messung.
 *
 * **Eine Grenze, gemessen am 3.9.2026:** `tools/` steht zwar in den Eingaben der Testaufgabe
 * (`app/build.gradle.kts`), aber Gradle vergleicht **Inhalte**, nicht Rechte. Nimmt jemand
 * nur das ausführbare Bit weg, läuft die Aufgabe nicht neu und die Regel bleibt grün, bis
 * etwas anderes sie auslöst. Mit `--rerun-tasks` fällt sie sofort um; nachgestellt und
 * gesehen. Für den Alltag reicht das — im selben Zug ändert sich fast immer auch Inhalt.
 */
class WerkzeugeTest {

    private val verzeichnis = File("../tools")
    private val beschreibung = File("../tools/README.md").readText()

    private fun werkzeuge(): List<File> = verzeichnis
        .listFiles { d -> d.extension == "sh" || d.extension == "py" }
        ?.sortedBy { it.name }
        .orEmpty()

    @Test
    fun `jedes Werkzeug ist beschrieben`() {
        val unbeschrieben = werkzeuge().map { it.name }.filterNot { "tools/$it" in beschreibung }
        assertEquals(
            "Werkzeug ohne Eintrag in tools/README.md - benutzt dann niemand mehr",
            emptyList<String>(),
            unbeschrieben,
        )
    }

    @Test
    fun `jedes beschriebene Werkzeug gibt es`() {
        val genannt = Regex("""tools/([a-z-]+\.(?:sh|py))""").findAll(beschreibung)
            .map { it.groupValues[1] }.toSortedSet()
        val fehlend = genannt.filterNot { File(verzeichnis, it).isFile }
        assertEquals("beschrieben, aber nicht vorhanden", emptyList<String>(), fehlend)
    }

    @Test
    fun `jedes Werkzeug laesst sich starten`() {
        werkzeuge().forEach {
            assertTrue("${it.name} ist nicht ausführbar", it.canExecute())
        }
    }
}
