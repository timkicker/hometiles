package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a picture message that does not arrive says so.
 *
 * `PLAN.md` 6 asks for it explicitly: report a failure visibly instead of swallowing it. the
 * receiver did the opposite - it noted that something had changed and kept quiet, so whoever
 * expected a picture waited for something that never came and thought the phone broken.
 *
 * the hint names both the reason and the way out: naming a problem without a way is only half
 * an answer.
 */
class MmsHintTest {

    private val receiver = Quelltext.file("dev/kicker/hometiles/sms/SmsComponents.kt").readText()
    private val notifications = Quelltext.file("dev/kicker/hometiles/sms/SmsNotifications.kt").readText()

    @Test
    fun `the wap push receiver is no longer silent`() {
        val place = Quelltext.cut(receiver, "class WapPushDeliverReceiver", "\n}")
        assertTrue(
            "WapPushDeliverReceiver no longer reports the picture message - then it arrives " +
                "quietly and nobody learns of it.",
            "showMmsHint(" in place,
        )
    }

    /**
     * `show` clears every channel with its own prefix away when creating one, on purpose, so
     * no old channel is left over per vibration length. an mms channel called `sms-…` would
     * vanish with the next incoming sms.
     */
    @Test
    fun `the mms channel does not fall to the clean-up loop`() {
        // the id is what matters, not what the constant is called: the rule fell over once
        // when `MMS_CHANNEL` became `MMS_CHANNEL_PREFIX` while the thing stayed the same.
        val channel = Regex("""MMS_CHANNEL(?:_PREFIX)? = "([^"]+)"""")
            .find(notifications)?.groupValues?.get(1)
        assertTrue("the mms channel no longer exists", channel != null)
        val prefix = Regex("""[^_]CHANNEL_PREFIX = "([^"]+)"""").find(notifications)!!.groupValues[1]
        assertEquals(
            "the mms channel starts with the prefix of the sms channels and is cleared away " +
                "with the next sms notice.",
            false,
            channel!!.startsWith(prefix),
        )
    }

    /**
     * the hint names a way out that also **works**. it used to say "open it in the phone's
     * messaging app", which was right while another app held the role - but only the default
     * app can fetch an mms, so once HomeTiles holds the role and does not fetch it, no other app
     * can either. the way that remains is giving the role back.
     *
     * the first version of this rule checked the words "network" and "messaging app" and
     * would have rejected the new, correct text: it hung on the wording, not on the thing.
     */
    @Test
    fun `the hint names a way out that still exists`() {
        listOf(
            "values" to Triple("settings", listOf("in the messaging app"), "cannot"),
            "values-de" to Triple("Einstellungen", listOf("in der Nachrichten-App"), "nicht"),
        ).forEach { (language, what) ->
            val (wayOut, deadEnds, reason) = what
            val text = Quelltext.textValue("mms_arrived_body", language)
            assertTrue(
                "$language: the hint names no way out - it has to point at the settings, " +
                    "where the role can be given back.",
                wayOut in text,
            )
            assertTrue(
                "$language: the hint names no reason.",
                reason in text,
            )
            deadEnds.forEach {
                assertTrue(
                    "$language: the hint sends to another messaging app. that one cannot " +
                        "fetch an mms while HomeTiles holds the role.",
                    it !in text,
                )
            }
        }
    }
}
