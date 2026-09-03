package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Anrede, nicht zwei.
 *
 * Die deutschen Texte sagten beides: 21 Stellen „Sie", 12 „Du" — und eine sagte beides in
 * einem Satz („Passende Nachrichten stehen nicht in **Deiner** Liste. **Sie** kommen
 * trotzdem an"), wo das zweite Wort dann nicht mehr eindeutig war: Anrede oder die
 * Nachrichten?
 *
 * Gewählt ist **„Sie"**, aus zwei Gründen: es war die Mehrheit, und die App wird für einen
 * älteren Menschen eingerichtet, den in seiner Sprache sonst auch niemand duzt. Wo es geht,
 * bleibt die beste Lösung, gar keine Anrede zu brauchen („Jede Uhrzeit steht für sich"
 * statt „Du siehst jede Uhrzeit für sich").
 *
 * Diese Regel ist eine **Entscheidung, keine Wahrheit**. Soll die App duzen, kehrt sich der
 * Test um — dann steht hier die andere Liste.
 */
class AddressFormTest {

    private val dateien = Quelltext.texte("values-de") + Quelltext.texte("values-de", "plurals.xml")

    /**
     * Auch klein geschrieben.
     *
     * Die erste Fassung sah nur „Du" gross an, mit der Begruendung, „du" stecke in „dazu"
     * und „durch". Das stimmt, aber die Wortgrenzen erledigen das ohnehin — und die sieben
     * Texte, die klein duzten, standen dadurch monatelang unbemerkt da. Am 3.9.2026
     * gefunden, beim Zaehlen der Anreden zu etwas ganz anderem.
     */
    private val duForm = Regex(
        """\b([Dd]u|[Dd]ir|[Dd]ich|[Dd]ein|[Dd]eine|[Dd]einen|[Dd]einem|[Dd]einer|[Dd]eines)\b""",
    )

    @Test
    fun `die deutschen Texte siezen`() {
        val treffer = mutableListOf<String>()
        dateien.forEach { datei ->
            datei.readLines().forEachIndexed { index, zeile ->
                if (duForm.containsMatchIn(zeile)) {
                    treffer += "${datei.name}:${index + 1}: ${zeile.trim()}"
                }
            }
        }
        assertTrue(
            "Hier wird geduzt, anderswo gesiezt - eine Anrede, nicht zwei:\n" +
                treffer.joinToString("\n"),
            treffer.isEmpty(),
        )
    }
}
