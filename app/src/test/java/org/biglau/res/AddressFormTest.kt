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

    /**
     * Und die Befehlsform, die ohne Fuerwort auskommt.
     *
     * „Oeffne sie in der Nachrichten-App", „Tippe einen Pfeil", „Waehle zuerst, was offen
     * bleibt" — drei Texte duzten, ohne ein einziges der Woerter oben zu enthalten. Sie
     * standen seit Wochen da; gefunden am 04.09.2026, als ich aus einem anderen Grund alle
     * Satzanfaenge durchsah.
     *
     * **Was diese Liste kann und was nicht:** sie kennt die Verben, mit denen diese App
     * Anweisungen gibt. Ein neues Verb faellt hier nicht auf. Das ist eine Schwaeche und
     * keine Absicht — wer eine Anweisung schreibt, die nicht in dieser Liste steht, muss
     * selbst daran denken. Die Liste ist besser als nichts und schlechter als eine
     * Grammatik.
     */
    private val befehlsform = Regex(
        """(Öffne|Tippe|Halte|Wähle|Schalte|Versuche|Drücke|Lege|Setze|Trage|Gib|Nimm|""" +
            """Schau|Prüfe|Wische|Starte|Warte|Lösche|Ändere|Speichere|Rufe|Sende|Ziehe|""" +
            """Klicke|Mache|Denke|Lass)""",
    )

    @Test
    fun `die deutschen Texte befehlen nicht in der Du-Form`() {
        val treffer = mutableListOf<String>()
        dateien.forEach { datei ->
            datei.readLines().forEachIndexed { index, zeile ->
                val inhalt = Regex(""">([^<]+)<""").find(zeile)?.groupValues?.get(1) ?: return@forEachIndexed
                // **Nur am Satzanfang.** „die Suche" und „eine Stelle" sind Hauptwoerter und
                // stehen mitten im Satz; die erste Fassung meldete sie und haette mich fast
                // dazu gebracht, richtige Texte zu aendern. Eine Befehlsform faengt an.
                inhalt.split(Regex("""(?<=[.!?—:])\s+""")).forEach { satz ->
                    val erstes = satz.trim().substringBefore(" ").trim(',', '.', ':', ';')
                    if (befehlsform.matches(erstes)) {
                        treffer += "${datei.name}:${index + 1}: $erstes …"
                    }
                }
            }
        }
        assertTrue(
            "Hier steht eine Anweisung in der Du-Form, ohne ein Fuerwort zu benutzen - " +
                "deshalb faellt sie der Regel oben nicht auf:\n" + treffer.joinToString("\n"),
            treffer.isEmpty(),
        )
    }

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
