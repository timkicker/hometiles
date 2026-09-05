package org.biglau.phone

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the incoming call screen must appear with the screen off.
 *
 * `startActivity` from the incall service worked while the app was in the foreground and
 * failed exactly where it counts: from android 10 on a background start is refused, and a
 * ringing phone in a pocket is the background case. what stands here is that the ringing goes
 * through a full screen intent, and that the notice does not outlive the ringing.
 */
class IncomingCallScreenTest {

    private val service = Quelltext.withoutComments("org/biglau/phone/BigInCallService.kt")
    private val notices = Quelltext.withoutComments("org/biglau/phone/CallNotifications.kt")

    @Test
    fun `a ringing call goes through the full screen intent`() {
        assertTrue(
            "CallNotifications no longer builds a full screen intent - then the call screen " +
                "stays away with the screen off.",
            "setFullScreenIntent" in notices,
        )
        val added = Quelltext.cut(service, "fun onCallAdded", "private fun setAudio")
        assertTrue(
            "onCallAdded no longer shows the notice for a ringing call: $added",
            "CallNotifications.showIncoming" in added,
        )
        assertTrue(
            "the ringing state is no longer told apart from an already running call - then " +
                "an answered call would post a notice too.",
            "Call.STATE_RINGING" in added,
        )
    }

    @Test
    fun `the notice goes when the ringing stops`() {
        assertTrue(
            "no CallNotifications.clear in onCallRemoved - the notice would stand after the " +
                "call has ended.",
            "CallNotifications.clear" in Quelltext.cut(service, "fun onCallRemoved", "override fun onCallAudioStateChanged"),
        )
        assertTrue(
            "no CallNotifications.clear on the state change - the notice would sit in the " +
                "shade through the whole conversation.",
            "CallNotifications.clear" in Quelltext.cut(service, "val callback", "override fun onCallAdded"),
        )
    }

    /**
     * the sms side deletes every channel whose id starts with `sms-`. a call channel with
     * that prefix would be swept away with the next vibration setting, and the ringing would
     * fall back to whatever android does by default.
     */
    @Test
    fun `the call channel does not carry the sms prefix`() {
        assertEquals(false, CallNotifications.CHANNEL.startsWith(org.biglau.sms.SmsNotifications.CHANNEL_PREFIX))
        assertEquals("call-incoming", CallNotifications.CHANNEL)
    }

    /**
     * the short answers must be readable before they are sent.
     *
     * they sat in rows fixed at 72 dp, like the buttons above them - and at this text size
     * two lines do not fit in 72 dp, so all three read "I cannot talk rig..." at the
     * emulator on 05.09.2026. a row that hides what it will send is worse than no row.
     */
    @Test
    fun `the reply rows may grow`() {
        // only the answer row, not the back row below it: that one is a button and stays at
        // its 72 dp.
        val row = Quelltext.cut(
            Quelltext.file("org/biglau/phone/InCallActivity.kt").readText(),
            "val body = stringResource(text)",
            "onClick = { onPick(body) }",
        )
        assertTrue(
            "the reply row is fixed in height again - then a sentence gets cut and one " +
                "sends what one could not read.",
            "heightIn(min = 72.dp)" in row && "Modifier.height(72.dp)" !in row,
        )
    }
}
