package org.biglau.settings

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Ließ sich nicht öffnen" und „ist keine Sicherung" sind zwei verschiedene Auskünfte.
 *
 * Am Emulator gesehen: eine Datei, die BigLau **selbst zwei Minuten vorher geschrieben
 * hatte**, wurde beim Öffnen von außen mit „Das ist keine BigLau-Sicherung" abgewiesen — in
 * Wahrheit ließ sie sich nur nicht lesen (eine Adresse, an die die App nicht heran darf).
 * Wer den Satz liest, sucht den Fehler in seiner Datei und wirft womöglich die einzige
 * Sicherung weg, die er hat.
 */
class ImportMessageTest {

    private val quelle = Quelltext.file("org/biglau/settings/ImportActivity.kt").readText()

    @Test
    fun `nicht lesbar und nicht lesbar-als-Sicherung sind zwei Faelle`() {
        val block = Quelltext.cut(quelle, "text = when {", "},")
        assertTrue("Der Lesefehler fehlt: $block", "transfer_unreadable" in block)
        assertTrue("Der Formatfehler fehlt: $block", "transfer_bad_file" in block)
        assertTrue(
            "Der Lesefehler muss zuerst geprüft werden, sonst verdeckt ihn der andere",
            block.indexOf("transfer_unreadable") < block.indexOf("transfer_bad_file"),
        )
    }

    @Test
    fun `beide Saetze stehen in beiden Sprachen`() {
        // Je Sprache, nicht je Datei: die Texte liegen inzwischen in mehreren Modulen,
        // und ein Satz gehoert in *eine* davon, nicht in jede.
        listOf("values", "values-de").forEach { sprache ->
            val texte = Quelltext.texts(sprache).joinToString("\n") { it.readText() }
            listOf("transfer_unreadable", "transfer_bad_file").forEach { name ->
                assertTrue("$sprache: $name fehlt", "\"$name\"" in texte)
            }
        }
    }
}
