package org.biglau.res

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ein Anführungszeichen, das nie ankommt.
 *
 * Android verschluckt ein **gerades** Anführungszeichen (`"`) in einem String, wenn es nicht
 * mit `\"` geschrieben ist. Am Bildschirm stand deshalb „Anna Bauer jetzt anrufen? — mit
 * offenem Anführungszeichen und ohne schließendes. Zehn deutsche Texte waren betroffen, die
 * ältesten seit Wochen: „%1$s" löschen?, Auf „%1$s" führt keine Kachel …
 *
 * Aufgefallen erst, als ich die Anrufliste auf Deutsch am Bildschirm ansah. Im Quelltext
 * sieht so ein String vollkommen richtig aus — genau die Sorte Fehler, die man nur am Gerät
 * findet.
 *
 * Die Regel: in Texten stehen **typografische** Anführungszeichen (Deutsch „…", Englisch
 * “…”). Sie werden nicht verschluckt und sehen besser aus.
 */
class QuotesTest {

    private val dateien = listOf(
        "src/main/res/values/strings.xml",
        "src/main/res/values-de/strings.xml",
        "src/main/res/values/plurals.xml",
        "src/main/res/values-de/plurals.xml",
    ).map(::File)

    @Test
    fun `kein gerades Anfuehrungszeichen in einem Text`() {
        val treffer = dateien.flatMap { datei ->
            Regex("""<(string |string>|item)[^>]*>((?:(?!</).)*)<""", RegexOption.DOT_MATCHES_ALL)
                .findAll(datei.readText())
                .filter { '"' in it.groupValues[2] }
                .map { "${datei.name}: ${it.groupValues[2].take(40)}" }
        }
        assertEquals(emptyList<String>(), treffer)
    }
}
