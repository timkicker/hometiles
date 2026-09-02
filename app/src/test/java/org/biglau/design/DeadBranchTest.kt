package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Verzweigung, deren beide Wege dasselbe tun, ist eine vergessene Absicht.
 *
 * Anlass: im Anrufbildschirm stand `if (view.speakerOn) CallAction.SPEAKER else
 * CallAction.SPEAKER`. Der Autor - ich - hatte die Unterscheidung gemeint und nur die eine
 * Hälfte hingeschrieben. Das Ergebnis war ein Knopf, der den Lautsprecher einschaltete und
 * ihn nie wieder ausschaltete: wer ihn versehentlich traf, hörte das Gespräch bis zum
 * Auflegen laut im Raum.
 *
 * Der Compiler sagt dazu nichts, und kein Test fiel darauf herein - beide Zweige lieferten
 * ja das erwartete Ergebnis. Auffallen kann es nur so.
 */
class DeadBranchTest {


    private val muster = Regex("""\bif\s*\(.+?\)\s+(.+?)\s+else\s+(.+)""")

    private fun dateien(): List<File> =
        Quelltext.dateien()

    /** Endekommas und schliessende Klammern gehoeren nicht zum Zweig. */
    private fun sauber(zweig: String): String = zweig.trim().trimEnd(',', ')')

    @Test
    fun `keine Verzweigung mit zwei gleichen Zweigen`() {
        val gleich = mutableListOf<String>()
        dateien().forEach { datei ->
            datei.readLines().forEachIndexed { index, zeile ->
                val treffer = muster.find(zeile) ?: return@forEachIndexed
                val links = sauber(treffer.groupValues[1])
                val rechts = sauber(treffer.groupValues[2])
                // Ein "if" im rechten Zweig ist eine Kette, kein doppelter Weg.
                if (rechts.startsWith("if")) return@forEachIndexed
                if (links.isNotEmpty() && links == rechts) {
                    gleich += "${datei.name}:${index + 1}: ${zeile.trim()}"
                }
            }
        }
        assertTrue(
            "Beide Zweige tun dasselbe - da fehlt die Hälfte einer Absicht:\n" +
                gleich.joinToString("\n"),
            gleich.isEmpty(),
        )
    }
}
