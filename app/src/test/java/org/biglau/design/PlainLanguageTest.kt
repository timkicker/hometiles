package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Sprach- und Schriftregeln aus `PLAN.md` 3.7, die noch keine hatten.
 *
 * Sie waren beim Nachsehen alle eingehalten — der Test hält sie fest, damit das so bleibt.
 * Ein „Ups!" schreibt sich schnell hinein, und wer es später liest, hält es für Absicht.
 */
class PlainLanguageTest {

    private val quellen = File("src/main/java").walkTopDown().filter { it.extension == "kt" }
    private val texte = listOf("src/main/res/values/strings.xml", "src/main/res/values-de/strings.xml")
        .map(::File)

    /**
     * „Keine Versalien für Kachelbeschriftungen. Großbuchstaben zerstören die Wortkontur,
     * an der Wenigleser sich orientieren."
     *
     * Ausnahme mit Grund: die Initialen im Kontaktbild und auf der Kontaktkachel — zwei
     * Buchstaben ohne Wortkontur, die es zu zerstören gäbe.
     */
    @Test
    fun `keine Versalien in der Anzeige`() {
        val treffer = quellen.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                if ("uppercase()" in zeile && datei.name != "ContactAvatar.kt") {
                    "${datei.name}:${index + 1}"
                } else {
                    null
                }
            }
        }.toList()
        assertEquals(emptyList<String>(), treffer)
    }

    /** „Fehlermeldungen sagen, was passiert ist und was zu tun ist. Kein ‚Ups!'." */
    @Test
    fun `keine Verlegenheitsfloskeln`() {
        val floskeln = listOf("Ups", "Oops", "Sorry", "Entschuldig", "Hoppla")
        val treffer = texte.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                val wort = floskeln.firstOrNull { it in zeile }
                if (wort != null) "${datei.name}:${index + 1} ($wort)" else null
            }
        }
        assertEquals(emptyList<String>(), treffer)
    }

    /**
     * Und kein Ausrufezeichen. Eine App, die ihren Nutzer anruft, klingt entweder aufgeregt
     * oder verkauft ihm etwas; beides gehört hier nicht hin.
     */
    @Test
    fun `kein Ausrufezeichen`() {
        val treffer = texte.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                if ("!" in zeile && "<!--" !in zeile) "${datei.name}:${index + 1}" else null
            }
        }
        assertEquals(emptyList<String>(), treffer)
    }
}
