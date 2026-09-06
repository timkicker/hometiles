package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * when something goes wrong, the reason stands beside it - as far as it is known.
 *
 * deleting from the call log and setting a favourite say only "did not work", and that is
 * the whole truth there: both sit behind a permission flow, so a failure afterwards means
 * the provider simply deleted nothing. sending caught every error in one `runCatching`, and
 * the one reason a person can fix - the missing permission - looked like a defect.
 *
 * deliberately no permission dialog from the sending place: it sends, it should not also
 * fetch the right to. the sentence names the place, the decision falls elsewhere.
 */
class ReasonGivenTest {

    private val sms = Quelltext.file("dev/kicker/hometiles/sms/SmsActivity.kt").readText()

    @Test
    fun `a failure while sending tells the missing permission apart`() {
        val place = Quelltext.cut(sms, "R.string.sms_send_failed").take(400) +
            Quelltext.cut(sms, "", "R.string.sms_send_failed").takeLast(700)
        assertTrue(
            "the failure while sending always shows the same sentence. the missing " +
                "SEND_SMS permission is the one reason one can fix - that belongs named.",
            "sms_send_no_permission" in place && "Manifest.permission.SEND_SMS" in place,
        )
    }

    @Test
    fun `the sending place fetches itself no permission to send`() {
        val send = Quelltext.cut(sms, "private fun send(")
        assertEquals(
            "a permission is requested in `send`. this place sends - it should not also " +
                "fetch the right to, otherwise a sent message stands at the end of a chain " +
                "of dialogs that nobody consciously triggered.",
            false,
            ".launch(Manifest.permission" in send,
        )
    }

    @Test
    fun `the hint names the place in both languages`() {
        listOf("values" to "app settings", "values-de" to "App-Einstellungen").forEach { (language, place) ->
            val text = Quelltext.textValue("sms_send_no_permission", language)
            assertTrue("$language: the hint does not name the place: $text", place in text)
        }
    }
}
