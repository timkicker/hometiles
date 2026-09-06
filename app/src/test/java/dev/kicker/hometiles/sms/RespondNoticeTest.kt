package dev.kicker.hometiles.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * rejecting a call with a message was a silent idle run: the service took the request, did
 * nothing and considered itself finished. no error appeared, and the caller got no message.
 */
class RespondNoticeTest {

    @Test
    fun `the text passed in stands in the notice`() {
        assertEquals(
            "on the train, will call back",
            RespondNotice.body("on the train, will call back", "Notice"),
        )
    }

    @Test
    fun `without a text the notice stands there`() {
        // a notice without content is worse than none.
        assertEquals("Notice", RespondNotice.body("", "Notice"))
        assertEquals("Notice", RespondNotice.body("   ", "Notice"))
    }

    @Test
    fun `space at the edges falls away`() {
        assertEquals("on the train", RespondNotice.body("  on the train  ", "Notice"))
    }

    @Test
    fun `one id per number`() {
        // the same number in two spellings is the same notice - otherwise two notices stack
        // up for the same caller. the spacing is what the id has to survive.
        assertEquals(
            RespondNotice.notificationId("+436601234567"),
            RespondNotice.notificationId("+43 660 123 4567"),
        )
        assertNotEquals(
            RespondNotice.notificationId("+436601234567"),
            RespondNotice.notificationId("+436601234568"),
        )
    }

    @Test
    fun `the id does not collide with a message's`() {
        // otherwise one notice would clear the other away.
        assertNotEquals(
            SmsNotifications.notificationId("+436601234567"),
            RespondNotice.notificationId("+436601234567"),
        )
    }
}
