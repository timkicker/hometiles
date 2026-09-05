package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Bedienelement mit leerer Handlung ist ein Versprechen ohne Gegenwert.
 *
 * Anlass: die Zeile „Neue Nachricht an …" — sie erscheint, wenn BigLau über einen
 * `smsto:`-Verweis geöffnet wird, sieht aus wie ein Knopf, ist hervorgehoben, und hatte
 * `onClick = {}`. Wer von einem Verweis kam, tippte darauf und **nichts geschah**.
 *
 * Beim Nachsehen fanden sich drei weitere Zeilen mit leerer Handlung, alle drei bloße
 * Auskünfte (die aktuelle Belegung, die aktuelle Größe, die Diagnosezeilen). Auch die sind
 * ein Fehler, wenn auch ein leiserer: sie schlucken den Tipp still, und die Vorlesefunktion
 * sagt sie als „Schaltfläche" an. `BigRow` kennt deshalb jetzt Zeilen **ohne** Handlung —
 * die sind dann weder anklickbar noch ein Knopf.
 */
class DeadControlTest {


    private val muster = Regex("""on(Click|LongClick|Pick|Confirm|Accept)\s*=\s*\{\s*\}""")

    private fun dateien(): List<File> =
        Quelltext.files()

    @Test
    fun `kein Bedienelement mit leerer Handlung`() {
        val leer = mutableListOf<String>()
        dateien().forEach { datei ->
            datei.readLines().forEachIndexed { index, zeile ->
                // Erklaerungen duerfen die Regel nennen, ohne sie zu brechen.
                val kommentar = zeile.trimStart().let {
                    it.startsWith("*") || it.startsWith("//") || it.startsWith("/*")
                }
                if (!kommentar && muster.containsMatchIn(zeile)) {
                    leer += "${datei.name}:${index + 1}: ${zeile.trim()}"
                }
            }
        }
        assertTrue(
            "Diese Bedienelemente tun nichts - entweder sie bekommen eine Handlung, oder " +
                "sie duerfen keine sein (BigRow ohne onClick):\n" + leer.joinToString("\n"),
            leer.isEmpty(),
        )
    }
}
