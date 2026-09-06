package dev.kicker.hometiles.notify

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever builds a full-screen notice must declare it too.
 *
 * `setFullScreenIntent` without `USE_FULL_SCREEN_INTENT` in the manifest throws no error -
 * the notice simply appears as an ordinary one, and nobody learns why the setting does
 * nothing.
 */
class FullScreenIntentTest {

    private val sources = Quelltext.files()
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `full-screen notice and declaration belong together`() {
        val builds = sources.any { "setFullScreenIntent" in it.readText() }
        val declared = "android.permission.USE_FULL_SCREEN_INTENT" in manifest
        assertTrue(
            "setFullScreenIntent stands in the source, USE_FULL_SCREEN_INTENT is missing " +
                "from the manifest",
            !builds || declared,
        )
        assertTrue(
            "USE_FULL_SCREEN_INTENT stands in the manifest, but nobody builds a full-screen " +
                "notice",
            !declared || builds,
        )
    }

    /**
     * only what the notice itself opened may go over the lock screen. with `showWhenLocked` in
     * the manifest a conversation opened by hand and left lying would sit over the lock screen
     * too - a wholly different promise.
     */
    @Test
    fun `the message view does not stand over the lock permanently`() {
        val block = Quelltext.cut(manifest, ".sms.SmsActivity", "</activity>")
        assertTrue("showWhenLocked does not belong in the manifest: $block", "showWhenLocked" !in block)
    }
}
