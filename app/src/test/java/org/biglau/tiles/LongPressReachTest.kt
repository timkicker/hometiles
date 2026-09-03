package org.biglau.tiles

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Jede Aktion muss auch auf den Langdruck passen.
 *
 * `PLAN.md` 4.3, Zeile 499: „Jede Aktion zusätzlich auf **Langdruck** belegbar, unabhängig
 * vom Kurzdruck." Zur Wahl standen aber nur Apps und eingebaute Funktionen — Kontakte,
 * Verknüpfungen, Screens und Webseiten fehlten, obwohl das Modell sie längst trägt und der
 * Startbildschirm sie ausführt. Ein halb eingelöstes Versprechen sieht von außen aus wie
 * eine Einstellung, die es nicht gibt.
 *
 * Seither führen beide Wege durch dieselben Auswahllisten. Der Preis dafür ist eine
 * Fallunterscheidung beim Schreiben — und genau die prüft der zweite Test: schriebe ein
 * Zweig wieder unmittelbar auf die Hauptaktion, überschriebe die Auswahl auf dem
 * Langdruckweg stumm das, was die Kachel bisher tat.
 */
class LongPressReachTest {

    private val quelle = Quelltext.datei("org/biglau/tiles/TileEditorActivity.kt").readText()

    @Test
    fun `jede Art laesst sich auch auf den Langdruck legen`() {
        val angeboten = Regex("""onPick\(Mode\.(\w+)\)""")
            .findAll(quelle)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            setOf(
                "PICK_APP", "PICK_CONTACT", "PICK_BUILTIN", "PICK_SHORTCUT_APP", "PICK_SCREEN",
                "EDIT_LINK", "EDIT_NUMBER",
            ),
            angeboten,
        )
    }

    /**
     * Widget und Ordner stehen absichtlich nicht zur Wahl: beide sind kein Griff, sondern
     * der Inhalt einer Zelle. Sie schreiben deshalb weiterhin unmittelbar — und nur sie.
     */
    @Test
    fun `die Auswahl schreibt nie an der Fallunterscheidung vorbei`() {
        val zeilen = quelle.lines()
        val fremde = zeilen.mapIndexedNotNull { index, zeile ->
            if (!zeile.contains("TileEdits.withAction(")) {
                null
            } else {
                val umfeld = zeilen.subList(index, minOf(index + 4, zeilen.size)).joinToString(" ")
                val erlaubt = "ButtonAction.Widget" in umfeld ||
                    "ButtonAction.Folder" in umfeld ||
                    "aufLangdruck" in zeilen.subList(maxOf(0, index - 4), index).joinToString(" ")
                if (erlaubt) null else "Zeile ${index + 1}: ${zeile.trim()}"
            }
        }
        assertEquals(emptyList<String>(), fremde)
    }
}
