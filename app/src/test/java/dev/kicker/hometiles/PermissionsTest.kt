package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every permission in the manifest is needed as well.
 *
 * a permission is a promise outwards: it stands in the phone's app info as "can: ...", and
 * it is the first thing someone looks at who is asked to trust a launcher with their
 * contacts and messages. one that is never used is a claim about abilities that do not
 * exist.
 */
class PermissionsTest {

    /** permission -> why it is needed although no source names it. */
    private val neededWithoutMention = mapOf(
        "RECEIVE_SMS" to
            "required for the default sms role. without it HomeTiles does not appear in the " +
            "choice at all; the system uses it, not we.",
        "USE_FULL_SCREEN_INTENT" to
            "carries `setFullScreenIntent` in SmsNotifications and in CallNotifications. " +
            "from android 14 on it has to be declared, otherwise the notice appears quietly " +
            "as an ordinary one.",
    )

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun permissions(): List<String> =
        Regex("""uses-permission android:name="android\.permission\.(\w+)"""")
            .findAll(manifest).map { it.groupValues[1] }.toList()

    @Test
    fun `no permission without a use`() {
        val source = Quelltext.files().joinToString("\n") { it.readText() }
        val unused = permissions()
            .filterNot { it in source }
            .filterNot { it in neededWithoutMention }
            .sorted()
        assertEquals(
            "this permission stands in the manifest but is used nowhere. it stands in the " +
                "user's app info as \"can: ...\" - either out with it, or into the list in " +
                "PermissionsTest with a reason.",
            emptyList<String>(),
            unused,
        )
    }

    @Test
    fun `every exception names its reason`() {
        neededWithoutMention.forEach { (name, reason) ->
            assertTrue("$name: reason missing or too short", reason.length > 40)
        }
        val unknown = neededWithoutMention.keys.filterNot { it in permissions() }
        assertEquals(
            "the list holds a permission the manifest no longer has - then the exception " +
                "can go too.",
            emptyList<String>(),
            unknown,
        )
    }

    /** and the number itself is a reminder: it should not grow unnoticed. */
    @Test
    fun `there are fourteen permissions`() {
        assertEquals(
            "the number of permissions has changed. each one is a promise outwards - that " +
                "belongs seen, not only translated.",
            14,
            permissions().size,
        )
    }
}
