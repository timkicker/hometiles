package org.biglau.res

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

    private val dateien = listOf(
        "src/main/res/values-de/strings.xml",
        "src/main/res/values-de/plurals.xml",
    )

    /** Gross geschrieben, damit „du" in „dazu" oder „durch" nicht mitzaehlt. */
    private val duForm = Regex("""\b(Du|Dir|Dich|Dein|Deine|Deinen|Deinem|Deiner|Deines)\b""")

    @Test
    fun `die deutschen Texte siezen`() {
        val treffer = mutableListOf<String>()
        dateien.forEach { pfad ->
            File(pfad).readLines().forEachIndexed { index, zeile ->
                if (duForm.containsMatchIn(zeile)) {
                    treffer += "${pfad.substringAfterLast('/')}:${index + 1}: ${zeile.trim()}"
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
