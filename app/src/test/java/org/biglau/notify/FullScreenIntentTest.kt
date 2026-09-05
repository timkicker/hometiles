package org.biglau.notify

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer eine Vollbild-Meldung baut, muss sie auch anmelden.
 *
 * `setFullScreenIntent` ohne `USE_FULL_SCREEN_INTENT` im Manifest wirft keinen Fehler — die
 * Meldung erscheint einfach als gewöhnliche, und niemand erfährt, warum die Einstellung
 * nichts tut. Genau die Sorte stiller Zusage, die diese App nicht haben soll.
 */
class FullScreenIntentTest {

    private val quellen = Quelltext.files()
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `Vollbild-Meldung und Anmeldung gehoeren zusammen`() {
        val baut = quellen.any { "setFullScreenIntent" in it.readText() }
        val angemeldet = "android.permission.USE_FULL_SCREEN_INTENT" in manifest
        assertTrue(
            "setFullScreenIntent steht im Quelltext, USE_FULL_SCREEN_INTENT fehlt im Manifest",
            !baut || angemeldet,
        )
        assertTrue(
            "USE_FULL_SCREEN_INTENT steht im Manifest, aber niemand baut eine Vollbild-Meldung",
            !angemeldet || baut,
        )
    }

    /**
     * Über den Sperrbildschirm darf nur, was die Meldung selbst geöffnet hat.
     *
     * Stünde `showWhenLocked` im Manifest, läge auch die von Hand geöffnete und liegen
     * gelassene Unterhaltung über dem Sperrbildschirm — eine ganz andere Zusage.
     */
    @Test
    fun `die Nachrichtenansicht steht nicht dauerhaft ueber dem Schloss`() {
        val block = Quelltext.cut(manifest, ".sms.SmsActivity", "</activity>")
        assertTrue("showWhenLocked gehört nicht ins Manifest: $block", "showWhenLocked" !in block)
    }
}
