package org.biglau.sms

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the default sms role's four required components stand in the manifest.
 *
 * android demands four things of a default sms app, all of them: a receiver for `SMS_DELIVER`,
 * one for `WAP_PUSH_DELIVER` (mms), a service for replying with a message, and an activity
 * for `SENDTO`. if **one** is missing, BigLau does not even appear in the choice of default
 * sms app - without an error, without a notice, without any test turning red.
 *
 * the README promises all four are there and `SmsComponents.kt` says so in a comment. nothing
 * checked it: the tests beside it check what the receivers *do*, not that they exist.
 */
class SmsRoleTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun contains(vararg parts: String) = parts.all { it in manifest }

    @Test
    fun `the receiver for incoming messages is there`() {
        assertTrue(
            "SMS_DELIVER is missing - BigLau then never gets to see incoming messages",
            contains(
                "android.provider.Telephony.SMS_DELIVER",
                "android.permission.BROADCAST_SMS",
            ),
        )
    }

    @Test
    fun `the receiver for mms is there`() {
        assertTrue(
            "WAP_PUSH_DELIVER or its mime type is missing",
            contains(
                "android.provider.Telephony.WAP_PUSH_DELIVER",
                "application/vnd.wap.mms-message",
                "android.permission.BROADCAST_WAP_PUSH",
            ),
        )
    }

    @Test
    fun `the service for replying during a call is there`() {
        assertTrue(
            "RESPOND_VIA_MESSAGE is missing - without it BigLau is not in the choice",
            contains(
                "android.intent.action.RESPOND_VIA_MESSAGE",
                "android.permission.SEND_RESPOND_VIA_MESSAGE",
            ),
        )
    }

    @Test
    fun `the activity for SENDTO knows all four schemes`() {
        // from the name attribute: a comment mentioning the name would otherwise set the cut
        // too early - that happened in the neighbouring test.
        val block = Quelltext.cut(manifest, "\".sms.SmsActivity\"", "</activity>")
        listOf("\"sms\"", "\"smsto\"", "\"mms\"", "\"mmsto\"").forEach { scheme ->
            assertTrue("SENDTO without scheme $scheme", scheme in block)
        }
        assertTrue("SENDTO is missing", "android.intent.action.SENDTO" in block)
    }
}
