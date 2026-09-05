package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer „false" zurueckgibt, muss auch gehoert werden.
 *
 * Anlass: am Emulator tat eine App-Kachel gar nichts. Die App dahinter war deinstalliert,
 * `AppRepository.launch` meldete das ordentlich mit `false` — nur sah niemand hin. Auf der
 * Kachel stand weiter der Name, das Tippen loeste nichts aus, und nichts erklaerte warum.
 * Fuer jemanden, der ohnehin unsicher ist, ob er das Telefon richtig bedient, ist eine
 * Kachel, die schweigend nichts tut, schlimmer als eine Fehlermeldung.
 *
 * Bei den Verknuepfungen war es von Anfang an richtig gemacht (`shortcut_gone`); nur bei
 * den Apps fehlte es an drei Stellen. Deshalb dieser Test: die Regel gilt fuer jede
 * Startfunktion, die ihr Scheitern meldet.
 */
class LaunchFailureTest {


    /** Aufrufe, deren Rueckgabe ausgewertet werden muss. */
    private val geprueft = listOf(".launch(")

    private fun dateien(): List<File> =
        Quelltext.files()

    @Test
    fun `jeder Startversuch wertet sein Ergebnis aus`() {
        val ungeprueft = mutableListOf<String>()
        dateien().forEach { datei ->
            // Die Funktion selbst zaehlt nicht als Aufrufstelle.
            if (datei.name == "AppRepository.kt" || datei.name == "ShortcutRepository.kt") return@forEach
            datei.readLines().forEachIndexed { index, zeile ->
                val ruf = geprueft.any { it in zeile }
                if (!ruf) return@forEachIndexed
                // Nur die Startfunktionen der beiden Verzeichnisse, nicht coroutine `launch`.
                if (!zeile.contains("packageName")) return@forEachIndexed
                // Der Rueckgabewert darf auch der Wert eines `when` sein, das ein paar
                // Zeilen darueber einem `val` zugewiesen wird. Am 03.09.2026 zogen die
                // beiden Startwege in eine gemeinsame Funktion; die Regel las weiter nur
                // die eine Zeile und meldete einen Fehler, den es nicht gab. Zum dritten
                // Mal an einem Tag: sie hing an der Form, nicht an der Sache.
                val zeilen = datei.readLines()
                val ausgewertet = zeile.contains("if (!") ||
                    zeile.contains("val ") ||
                    zeile.contains("return ") ||
                    zeilen.subList(maxOf(0, index - 4), index)
                        .any { Regex("""(val \w+ =|return) when""").containsMatchIn(it) }
                if (!ausgewertet) ungeprueft += "${datei.name}:${index + 1}: ${zeile.trim()}"
            }
        }
        assertTrue(
            "Diese Startversuche werfen ihr Ergebnis weg - die Kachel taete dann schweigend " +
                "nichts:\n" + ungeprueft.joinToString("\n"),
            ungeprueft.isEmpty(),
        )
    }
}
