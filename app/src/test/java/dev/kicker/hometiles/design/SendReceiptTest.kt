package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "sent" has to be a fact, not a hope.
 *
 * `SmsManager.sendTextMessage` returns at once. whether the network took the message at all
 * is settled only seconds later and arrives as a broadcast - **if** a `PendingIntent` is
 * passed. until 03.09.2026 `null` stood there: HomeTiles reported "sent" because the call had
 * not thrown, wrote the message into the outbox, and a message the network had refused
 * looked like any other afterwards.
 *
 * for a phone somebody relies on that is the worse of the two cases: not being able to send
 * is a problem, but believing one has sent is a problem one does not know about. `PLAN.md` P6
 * has named delivery reports from the start; the source did not even have the send receipt.
 */
class SendReceiptTest {

    private val sending = Quelltext.withoutComments("dev/kicker/hometiles/sms/SmsActivity.kt")

    @Test
    fun `no message goes out without a receipt`() {
        val without = sending.lines().withIndex()
            .filter { (_, line) -> "sendTextMessage(" in line || "sendMultipartTextMessage(" in line }
            .filter { (i, _) ->
                // the call spans several lines; the receipt may stand in the next eight.
                sending.lines().drop(i).take(8).none { "receipt" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "a message goes out without a receipt here. then sent means only that the call " +
                "did not throw - and a refused message looks like one that arrived.",
            emptyList<Int>(),
            without,
        )
    }

    @Test
    fun `the row stands there before the sending`() {
        val whileWriting = sending.indexOf("Telephony.Sms.Sent.CONTENT_URI")
        val whileSending = sending.indexOf("sendTextMessage(")
        assertTrue("the outbox is no longer written", whileWriting > 0)
        assertTrue(
            "writing happens only after sending. then the receipt cannot say **which** " +
                "message did not get through.",
            whileWriting < whileSending,
        )
    }

    @Test
    fun `the receipt turns the failure into something visible`() {
        val receiver = Quelltext.withoutComments("dev/kicker/hometiles/sms/SmsSentReceiver.kt")
        assertTrue(
            "the failure is not noted in the database - then the message keeps standing " +
                "there as if it were out.",
            "MESSAGE_TYPE_FAILED" in receiver,
        )
        assertTrue(
            "nobody learns of it. whoever tapped send has usually left the screen already - " +
                "a notice inside the conversation would only be seen by someone already " +
                "looking.",
            "showSendFailed" in receiver,
        )
        assertTrue(
            "the reason is not named. a general error is no information; no reception is.",
            "RESULT_ERROR_NO_SERVICE" in receiver,
        )
    }

    @Test
    fun `an unsent message looks different`() {
        assertTrue(
            "nothing shows on an unsent message in the conversation. it stands in the same " +
                "row in the same place - the only difference would be that no answer comes.",
            "message.failed" in sending && "R.string.sms_not_sent" in sending,
        )
    }
}
