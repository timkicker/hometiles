package org.biglau.security

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Auf dem Schloss steht der Ausweg, nicht die Begründung.
 *
 * Am Bildschirm gesehen: der lange Erklärtext passte auf der PIN-Eingabe in drei Zeilen und
 * wurde **genau an der Stelle abgeschnitten**, an der der Ausweg steht — „… Wenn Sie sie …".
 * Wer die PIN vergessen hat, findet dort also gerade den Satz nicht, den er braucht.
 *
 * Auf der Einstellungsseite bleibt der lange Text; dort ist Platz, und dort liest man ihn,
 * bevor man eine PIN setzt.
 */
class PinGateHintTest {

    private val einstellungen =
        File("src/main/java/org/biglau/settings/SettingsActivity.kt").readText()

    @Test
    fun `das Schloss zeigt den kurzen Satz`() {
        val stelle = einstellungen.substringAfter("Page.GATE -> PinGate(").substringBefore("wrongText")
        assertTrue("Der kurze Satz fehlt: $stelle", "security_forgot" in stelle)
        assertTrue("Der lange Text steht wieder auf dem Schloss: $stelle", "security_explainer" !in stelle)
    }

    @Test
    fun `der kurze Satz nennt die dreissig Sekunden`() {
        listOf("src/main/res/values/strings.xml", "src/main/res/values-de/strings.xml").forEach { pfad ->
            val text = Regex("""<string name="security_forgot">([^<]*)</string>""")
                .find(File(pfad).readText())?.groupValues?.get(1)
            assertTrue("$pfad: security_forgot fehlt", text != null)
            assertTrue("$pfad: ohne die Dauer nützt der Satz nichts", "30" in text!!)
            assertTrue("$pfad: zu lang für das Schloss (${text.length})", text.length <= 60)
        }
    }
}
