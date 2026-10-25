package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a call can always be taken, seen and worked with keys. issue #2, a cat s22 flip, measured
 * with the probe call on the emulator on 25.10.2026:
 *
 * - with the screen on the ringing showed as a small notice with no buttons,
 * - a call taken without the call screen ran with no screen and no notice,
 * - the number keys reached a call only once "keypad" had been chosen, and that keypad had
 *   no star and no pound, which a mailbox pin ends on.
 */
class CallReachTest {

    private val service = Quelltext.withoutComments("dev/kicker/hometiles/phone/BigInCallService.kt")
    private val notices = Quelltext.withoutComments("dev/kicker/hometiles/phone/CallNotifications.kt")
    private val screen = Quelltext.withoutComments("dev/kicker/hometiles/phone/InCallActivity.kt")
    private val receiver = Quelltext.withoutComments("dev/kicker/hometiles/phone/CallActionReceiver.kt")

    @Test
    fun `the ringing notice carries answer and reject`() {
        assertTrue("the notice has no answer button", "CallActionReceiver.ANSWER" in notices)
        assertTrue("the notice has no reject button", "CallActionReceiver.REJECT" in notices)
        assertTrue("answer does not answer", "ANSWER -> InCallRepository.answer()" in receiver)
        assertTrue("reject does not reject", "REJECT -> InCallRepository.reject()" in receiver)
    }

    /**
     * the call screen opened directly, and the ringing notice lay over it as a banner and
     * hid the caller's name. while the screen is in front the notice moves to a quiet channel;
     * without it the full screen intent stays, which wakes a sleeping phone.
     */
    @Test
    fun `the notice stays off the call screen`() {
        assertTrue("the call screen does not report itself", "CallNotifications.screenShown(this, true)" in screen)
        assertTrue("the call screen does not report leaving", "CallNotifications.screenShown(this, false)" in screen)
        assertTrue("the quiet channel is not used", "if (screenShown) CHANNEL_SHOWN else CHANNEL" in notices)
        assertTrue("without the screen the full screen intent is gone", "if (!screenShown) builder.setFullScreenIntent" in notices)
        assertTrue(
            "the quiet channel carries the sms prefix and would be swept away with it",
            !CallNotifications.CHANNEL_SHOWN.startsWith(dev.kicker.hometiles.sms.SmsNotifications.CHANNEL_PREFIX),
        )
    }

    @Test
    fun `a call taken anywhere opens the call screen`() {
        val change = Quelltext.cut(service, "val callback", "override fun onCallAdded")
        assertTrue(
            "a ringing call that turns active does not open the call screen: $change",
            "Call.STATE_ACTIVE && ringing.remove(call)) openCallScreen()" in change,
        )
        val added = Quelltext.cut(service, "fun onCallAdded", "private fun openCallScreen")
        assertTrue("a ringing call is not remembered as ringing", "ringing.add(call)" in added)
    }

    @Test
    fun `the number keys send tones during a call`() {
        assertTrue("the call screen does not listen to the number keys", "ScreenKeys(enabled = !showKeypad" in screen)
        assertTrue("a key sends no tone", "InCallRepository.playDigit(tone)" in screen)
        assertTrue("the call keypad has no star", "extraKey = '*'" in screen)
        assertTrue("the call keypad has no pound", "lastKey = '#'" in screen)
    }
}
