package dev.kicker.hometiles.res

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the diagnostics page has to name every dangerous permission.
 *
 * it is the answer to "it does not work" - there is no `adb` on a person's phone. a
 * permission missing there is a missing answer: on 02.09.2026 the call log was visible but
 * nothing could be deleted from it because `WRITE_CALL_LOG` was missing, and the page one
 * opens for that did not show exactly this row.
 */
class DiagnosticsCoverageTest {

    private val manifest = File("src/main/AndroidManifest.xml")
    private val page = Quelltext.file("dev/kicker/hometiles/settings/Diagnostics.kt")

    /** only this group is granted at runtime; the rest comes with the install. */
    private val dangerous = setOf(
        "CALL_PHONE", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_PHONE_STATE",
        "READ_CONTACTS", "WRITE_CONTACTS", "SEND_SMS", "READ_SMS", "RECEIVE_SMS",
        "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
    )

    /**
     * permissions the page need not name one by one.
     *
     * `READ_PHONE_STATE` stood here until 04.09.2026 with the reason that it was
     * deliberately not granted. looked at on the user's device it was granted - HomeTiles asks
     * for it so the signal bars show something. the reason was a measured fact that had
     * aged; the row now stands on the page.
     */
    private val exceptions = mapOf(
        "ACCESS_COARSE_LOCATION" to "stands on one row together with ACCESS_FINE_LOCATION",
        "WRITE_CONTACTS" to "hangs on the same question as READ_CONTACTS",
        "RECEIVE_SMS" to "hangs on the sms role, which the page shows as default sms app",
    )

    private fun declared(): Set<String> =
        Regex("""uses-permission android:name="android\.permission\.([A-Z_]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it in dangerous }
            .toSet()

    @Test
    fun `every dangerous permission stands on the diagnostics page`() {
        val text = page.readText()
        val missing = (declared() - exceptions.keys)
            .filterNot { text.contains("Manifest.permission.$it") }
            .sorted()
        assertEquals(
            "the diagnostics do not name these permissions - whoever wants to know why " +
                "something does not work finds no answer there: $missing",
            emptyList<String>(),
            missing,
        )
    }

    @Test
    fun `every exception names its reason and really exists`() {
        val present = declared()
        exceptions.forEach { (name, reason) ->
            assertTrue("$name no longer stands in the manifest", name in present)
            assertTrue("$name needs a reason", reason.length > 20)
        }
    }
}
