package dev.kicker.hometiles.notify

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the notification listener's name must not change.
 *
 * the permission for notification access lives **outside the app**: android remembers it in
 * `Settings.Secure.enabled_notification_listeners` as a full class name. if the class moves
 * to another package the entry points at nothing - the permission is silently gone, the
 * tiles never blink again, and nobody gets a message. only the user can restore it, in the
 * system settings.
 *
 * on 03.09.2026 four files moved from `notify` to `sms`, one of them a receiver named in the
 * manifest. the listener was not among them; nobody had checked. `ManifestClassesTest` would
 * have noticed the move, but only because the manifest would have come along - the line in
 * the user's system settings comes along with nothing.
 */
class ListenerNameTest {

    private val name = "dev.kicker.hometiles.notify.BigNotificationListener"

    @Test
    fun `the listener is still called the same`() {
        // through Quelltext.file, not a hand-built path: otherwise the rule hangs on the
        // module, which QuelltextTest forbids.
        val found = runCatching { Quelltext.file("${name.replace('.', '/')}.kt") }.isSuccess
        assertTrue(
            "the notification listener has moved or been renamed. the user's permission " +
                "stands in the system settings under the old name and is thereby silently " +
                "lost - they would have to grant it again by hand.",
            found,
        )
    }

    @Test
    fun `the manifest names exactly this name`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "manifest and class name have drifted apart",
            ".notify.BigNotificationListener" in manifest,
        )
    }
}
