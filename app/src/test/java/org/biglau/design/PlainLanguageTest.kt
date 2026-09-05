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

    private val quellen = Quelltext.files()
    /**
     * Beide Sprachen **und** die Mehrzahlformen.
     *
     * Bis zum 3.9.2026 stand hier nur `strings.xml`. Die Mehrzahltexte („%1$d Kacheln gehen
     * verloren") standen genauso auf dem Bildschirm und waren von jeder Regel hier
     * ausgenommen. `Quelltext.allTexts` fragt beides.
     */
    private val texte = Quelltext.allTexts()

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
        // Ueber die Sprachen und nicht ueber die gefundenen Dateien: `mapNotNull` liesse
        // die Regel gruen, wenn es den Text nirgends mehr gibt - sie haette dann nichts
        // angesehen. `textWert` faellt in dem Fall um.
        val zulang = listOf("values", "values-de").mapNotNull { sprache ->
            val text = Quelltext.textValue("empty_tile_invite", sprache)
            if (text.length > 12) "$sprache: \"$text\" (${text.length})" else null
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


}
