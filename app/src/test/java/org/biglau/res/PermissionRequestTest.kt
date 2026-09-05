package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever declares a dangerous permission in the manifest has to ask for it too.
 *
 * `WRITE_CALL_LOG` stood in the manifest, was checked before every deletion - and asked for
 * by no line. on the user's phone it was `granted=false` (02.09.2026), so whoever deleted a
 * call there confirmed the question and saw the row still standing: a dead end nothing led
 * out of, because nothing asked. the emergency call had the same gap with the location - the
 * message promised coordinates and went out without them.
 *
 * a declared permission without a question is always one of those two dead ends. it is
 * declared, after all, because some place in the code needs it.
 */
class PermissionRequestTest {

    private val manifest = File("src/main/AndroidManifest.xml")

    /** only this group is asked at runtime; the rest is granted when installing. */
    private val dangerous = setOf(
        "CALL_PHONE", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_PHONE_STATE",
        "READ_CONTACTS", "WRITE_CONTACTS", "SEND_SMS", "READ_SMS", "RECEIVE_SMS",
        "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
    )

    /**
     * permissions that cannot be asked for because they hang on a role.
     *
     * android grants `RECEIVE_SMS` to the app holding the sms role and to it alone - a dialog
     * of its own does not exist. whoever does not hold the role does not get the permission
     * after any question either.
     */
    private val throughARole = setOf("RECEIVE_SMS")

    private fun declared(): Set<String> =
        Regex("""uses-permission android:name="android\.permission\.([A-Z_]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it in dangerous }
            .toSet()

    /** everything standing in any `launch(…)` - singly or inside an `arrayOf(…)`. */
    private fun asked(): Set<String> {
        val hits = mutableSetOf<String>()
        Quelltext.files().forEach { file ->
            val text = file.readText()
            Regex("""\.launch\(""").findAll(text).forEach { start ->
                // read to the closing bracket of the launch call, so an arrayOf(…) spanning
                // several lines is taken along.
                var depth = 1
                var i = start.range.last + 1
                while (i < text.length && depth > 0) {
                    when (text[i]) {
                        '(' -> depth++
                        ')' -> depth--
                    }
                    i++
                }
                Regex("""Manifest\.permission\.([A-Z_]+)""")
                    .findAll(text.substring(start.range.last + 1, i))
                    .forEach { hits += it.groupValues[1] }
            }
        }
        return hits
    }

    @Test
    fun `every dangerous permission is asked for too`() {
        val missing = (declared() - asked() - throughARole).sorted()
        assertEquals(
            "these permissions stand in the manifest and are never asked for. whoever needs " +
                "them stands in front of a dead end: $missing",
            emptyList<String>(),
            missing,
        )
    }

    @Test
    fun `the rule reads permissions from an arrayOf too`() {
        // counter-check: the wizard asks READ_CONTACTS and CALL_PHONE only as a group.
        // without the bracket counting the rule would find neither.
        assertTrue("CALL_PHONE stands in an arrayOf", "CALL_PHONE" in asked())
    }

    @Test
    fun `the rule invents no hits`() {
        assertTrue("READ_CALENDAR is asked for nowhere", "READ_CALENDAR" !in asked())
    }
}
