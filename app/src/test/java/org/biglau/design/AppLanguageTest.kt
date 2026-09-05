package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Texte in der Sprache der App, nicht in der des Telefons.
 *
 * `AppLocale.wrap` hing nur an `BigLauActivity`. Alles ausserhalb einer Activity — Meldungen,
 * Wecker, der Vorgabetext des Notrufs — holte seine Texte aus dem rohen Context, also in der
 * Systemsprache. Am Emulator gesehen: Oberfläche auf Deutsch, die Meldung darüber auf
 * Englisch („Message not sent yet").
 *
 * Das trifft genau den Fall, für den es diese Einstellung gibt: ein Telefon, dessen
 * Systemsprache jemand anderes gesetzt hat — der Sohn beim Einrichten, der Händler, die
 * Werkseinstellung. Wer sie ändern wollte, müsste sich durch Systemeinstellungen in einer
 * Sprache arbeiten, die er nicht liest.
 */
class AppLanguageTest {


    private fun dateien(): List<File> =
        Quelltext.files()

    /**
     * Innerhalb einer Activity hängt die Sprache schon am Context (`attachBaseContext`),
     * und Composables lesen über `stringResource`. Geprüft wird der Rest.
     */
    private fun ausserhalbEinerActivity(datei: File): Boolean =
        !datei.name.endsWith("Activity.kt") && !datei.path.contains("/ui/")

    @Test
    fun `Texte ausserhalb einer Activity gehen durch AppLocale`() {
        val roh = mutableListOf<String>()
        dateien().filter(::ausserhalbEinerActivity).forEach { datei ->
            val inhalt = datei.readText()
            // Welche Namen in dieser Datei fuer einen gewrappten Context stehen.
            val erlaubt = Regex("""val (\w+) = AppLocale\.forApp\(""")
                .findAll(inhalt)
                .map { it.groupValues[1] }
                .toSet()
            datei.readLines().forEachIndexed { index, zeile ->
                // **Beide** Wege zu einem Text, nicht nur einer. Bis zum 04.09.2026 sah
                // diese Regel nur `getString` an; `getQuantityString` holt genauso einen
                // Text und kam genauso aus dem rohen Context. Aufgefallen, als eine neue
                // Meldung auf der SOS-Seite beides benutzte und nur die eine Haelfte
                // gemeldet wurde.
                val treffer = Regex("""(\w+)?\.?(?:resources\.)?get(?:String|QuantityString)\(R\.(?:string|plurals)""")
                    .find(zeile)
                    ?: return@forEachIndexed
                val empfaenger = treffer.groupValues[1]
                val gewrappt = empfaenger in erlaubt ||
                    "AppLocale.forApp" in zeile
                if (!gewrappt) roh += "${datei.name}:${index + 1}: ${zeile.trim()}"
            }
        }
        assertTrue(
            "Diese Texte kaemen in der Sprache des Telefons statt der der App:\n" +
                roh.joinToString("\n"),
            roh.isEmpty(),
        )
    }
}
