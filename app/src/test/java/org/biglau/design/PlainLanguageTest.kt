package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Sprach- und Schriftregeln aus `PLAN.md` 3.7, die noch keine hatten.
 *
 * Sie waren beim Nachsehen alle eingehalten — der Test hält sie fest, damit das so bleibt.
 * Ein „Ups!" schreibt sich schnell hinein, und wer es später liest, hält es für Absicht.
 */
class PlainLanguageTest {

    private val quellen = Quelltext.dateien()
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

    /**
     * Der Hinweis auf einer leeren Kachel bleibt kurz.
     *
     * Er steht in der Beschriftungszone einer Kachel und wird abgeschnitten wie jede andere
     * Beschriftung. Am Bildschirm gesehen: „Antippen zum Belegen" stand schon auf dem
     * 2 × 4-Raster als „Antippen zum Beleg…" da — ausgerechnet der eine Text, den ein neuer
     * Nutzer lesen muss. Bei vier Spalten passen rund zehn bis elf Zeichen in die Zeile;
     * zwölf sind die Grenze, an der es auf jedem Raster noch steht.
     */
    @Test
    fun `der Hinweis auf einer leeren Kachel bleibt kurz`() {
        val zulang = texte.mapNotNull { datei ->
            val text = Regex("""<string name="empty_tile_invite">([^<]*)</string>""")
                .find(datei.readText())?.groupValues?.get(1)
            if (text != null && text.length > 12) "${datei.name}: \"$text\" (${text.length})" else null
        }
        assertEquals(emptyList<String>(), zulang)
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
