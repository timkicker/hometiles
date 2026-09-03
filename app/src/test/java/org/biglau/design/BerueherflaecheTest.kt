package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Zwei Zusagen aus `PLAN.md` 3.6, die bisher niemand geprüft hat.
 *
 * **„Alle Dialoge als Vollbild-Großbutton-Dialoge, nicht als Material-AlertDialog."** Ein
 * `AlertDialog` bringt kleine Textknöpfe mit, die sich nicht vergrössern lassen — auf einem
 * Gerät für schlechte Augen ist das genau der Knopf, den man nicht trifft.
 *
 * **„Kein Element unter 48 dp."** Am 3.9.2026 nachgemessen: es gibt keines. Der einzige
 * Verdachtsfall war ein 32-dp-Symbol **in** einer 56-dp-Fläche — die Fläche zählt, nicht das
 * Bild darin. Genau deshalb sieht diese Regel auf die Modifier-Kette, in der auch
 * `clickable` steht, und nicht auf jede Grössenangabe im Umkreis.
 */
class BerueherflaecheTest {

    @Test
    fun `kein Material-AlertDialog`() {
        val treffer = Quelltext.dateien()
            .filter { datei ->
                datei.readLines().any { zeile ->
                    "AlertDialog" in zeile && !zeile.trim().startsWith("*") &&
                        !zeile.trim().startsWith("//")
                }
            }
            .map { it.name }
        assertEquals(
            "PLAN.md 3.6: Dialoge sind Vollbild-Bildschirme mit grossen Knöpfen. Ein " +
                "AlertDialog bringt kleine Textknöpfe mit, die sich nicht vergrössern lassen.",
            emptyList<String>(),
            treffer,
        )
    }

    @Test
    fun `keine klickbare Flaeche unter 48 dp`() {
        val muster = Regex("""\.size\((\d+(?:\.\d+)?)\.dp\)""")
        val zuKlein = Quelltext.dateien().flatMap { datei ->
            val zeilen = datei.readLines()
            zeilen.withIndex()
                .filter { (_, zeile) -> ".clickable(" in zeile || ".combinedClickable(" in zeile }
                .mapNotNull { (i, _) ->
                    // Die Modifier-Kette drumherum: zusammenhaengende Zeilen, die mit
                    // einem Punkt anfangen. Nur dort zaehlt eine Groesse zur Flaeche.
                    var anfang = i
                    while (anfang > 0 && zeilen[anfang - 1].trim().startsWith(".")) anfang--
                    var ende = i
                    while (ende + 1 < zeilen.size && zeilen[ende + 1].trim().startsWith(".")) ende++
                    zeilen.subList(anfang, ende + 1)
                        .mapNotNull { muster.find(it)?.groupValues?.get(1)?.toFloat() }
                        .filter { it < 48f }
                        .minOrNull()
                        ?.let { "${datei.name}:${i + 1} (${it.toInt()} dp)" }
                }
        }
        assertEquals(
            "PLAN.md 3.6: kein Element unter 48 dp. Ein Symbol darf kleiner sein - die " +
                "Fläche, die den Finger annimmt, nicht.",
            emptyList<String>(),
            zuKlein,
        )
    }
}
