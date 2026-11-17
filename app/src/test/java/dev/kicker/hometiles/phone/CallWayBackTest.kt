package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a running call can always be found again.
 *
 * the only notice belonged to the ringing and went once the call was taken. whoever then
 * pressed home or switched apps had no way back to the call screen: no hanging up, no mute,
 * no keypad (user mail 14.11.2026, played through with the probe call on the emulator).
 */
class CallWayBackTest {

    private val service = Quelltext.withoutComments("dev/kicker/hometiles/phone/BigInCallService.kt")
    private val notices = Quelltext.withoutComments("dev/kicker/hometiles/phone/CallNotifications.kt")

    @Test
    fun `every published call state passes the running notice`() {
        val publish = Quelltext.cut(service, "private fun publish(call: Call)", "private fun ")
        assertTrue("publishing a call does not update the running notice.", "ongoingNotice(call)" in publish)
        val notice = Quelltext.cut(service, "private fun ongoingNotice(call: Call)", "private fun ")
        listOf("CallStatus.ACTIVE", "CallStatus.HOLDING", "CallStatus.DIALING", "CallStatus.CONNECTING").forEach {
            assertTrue("a call in $it gets no running notice.", it in notice)
        }
    }

    @Test
    fun `the notice leads back and hangs up`() {
        val show = Quelltext.cut(notices, "fun showOngoing(", "fun clearOngoing(")
        assertTrue("the running notice can be swiped away.", ".setOngoing(true)" in show)
        assertTrue("the running notice does not open the call screen.", ".setContentIntent(screen)" in show)
        assertTrue("the running notice cannot hang up.", "CallActionReceiver.HANG_UP" in show)
        val receiver = Quelltext.withoutComments("dev/kicker/hometiles/phone/CallActionReceiver.kt")
        assertTrue("the receiver ignores the hang up.", "HANG_UP -> InCallRepository.hangUp()" in receiver)
    }

    @Test
    fun `the notice goes with the last call`() {
        val removed = Quelltext.cut(service, "override fun onCallRemoved(", "override fun ")
        assertTrue("the running notice outlives the call.", "CallNotifications.clearOngoing(this)" in removed)
    }

    @Test
    fun `the channel survives the sms clean up`() {
        assertFalse(
            CallNotifications.CHANNEL_ONGOING.startsWith(dev.kicker.hometiles.sms.SmsNotifications.CHANNEL_PREFIX),
        )
    }
}
