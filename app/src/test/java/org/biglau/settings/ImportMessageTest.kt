package org.biglau.settings

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

    private val quelle = File("src/main/java/org/biglau/settings/ImportActivity.kt").readText()

    @Test
    fun `nicht lesbar und nicht lesbar-als-Sicherung sind zwei Faelle`() {
        val block = quelle.substringAfter("text = when {").substringBefore("},")
        assertTrue("Der Lesefehler fehlt: $block", "transfer_unreadable" in block)
        assertTrue("Der Formatfehler fehlt: $block", "transfer_bad_file" in block)
        assertTrue(
            "Der Lesefehler muss zuerst geprüft werden, sonst verdeckt ihn der andere",
            block.indexOf("transfer_unreadable") < block.indexOf("transfer_bad_file"),
        )
    }

    @Test
    fun `beide Saetze stehen in beiden Sprachen`() {
        listOf("src/main/res/values/strings.xml", "src/main/res/values-de/strings.xml").forEach { pfad ->
            val texte = File(pfad).readText()
            listOf("transfer_unreadable", "transfer_bad_file").forEach { name ->
                assertTrue("$pfad: $name fehlt", "\"$name\"" in texte)
            }
        }
    }
}
