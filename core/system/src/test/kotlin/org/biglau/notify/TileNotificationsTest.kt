package org.biglau.notify

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileNotificationsTest {

    private val system = SystemPackages(sms = "com.sms.app", dialer = "com.dialer.app")

    @Test
    fun `an app tile watches its own app`() {
        val action = ButtonAction.App("com.chat", "com.chat.Main")
        assertEquals("com.chat", TileNotifications.watchedPackage(action, system))
    }

    @Test
    fun `the messages tile follows the chosen default app`() {
        // switching sms app keeps the tile right without anyone changing a setting.
        val action = ButtonAction.Action(Builtin.MESSAGES)
        assertEquals("com.sms.app", TileNotifications.watchedPackage(action, system))
        assertEquals("andere.sms", TileNotifications.watchedPackage(action, SystemPackages(sms = "andere.sms")))
    }

    /**
     * as soon as BigLau holds the dialer role, the default dialer is BigLau itself - and
     * BigLau reports **messages**, so the phone tile would have blinked on an incoming sms.
     */
    @Test
    fun `the phone tile follows no foreign app any more`() {
        val action = ButtonAction.Action(Builtin.DIALER)
        assertNull(TileNotifications.watchedPackage(action, system))
    }

    @Test
    fun `without a default app nothing is watched`() {
        assertNull(TileNotifications.watchedPackage(ButtonAction.Action(Builtin.MESSAGES), SystemPackages()))
    }

    @Test
    fun `other builtin actions watch nothing`() {
        listOf(Builtin.CAMERA, Builtin.SETTINGS, Builtin.APP_LIST, Builtin.SOS).forEach { builtin ->
            assertNull(TileNotifications.watchedPackage(ButtonAction.Action(builtin), system))
        }
    }

    @Test
    fun `contact and screen tiles watch nothing`() {
        assertNull(TileNotifications.watchedPackage(ButtonAction.Contact("Anna", "+431"), system))
        assertNull(TileNotifications.watchedPackage(ButtonAction.GoToScreen("s2"), system))
        assertNull(TileNotifications.watchedPackage(ButtonAction.None, system))
    }

    @Test
    fun `the count comes from the watched package`() {
        val button = Button(ButtonAction.Action(Builtin.MESSAGES))
        val counts = mapOf("com.sms.app" to 3, "com.chat" to 9)
        assertEquals(3, TileNotifications.badgeFor(button, counts, system))
    }

    @Test
    fun `a tile with blinking switched off does not count`() {
        val button = Button(ButtonAction.Action(Builtin.MESSAGES), blink = false)
        assertEquals(0, TileNotifications.badgeFor(button, mapOf("com.sms.app" to 3), system))
    }

    @Test
    fun `without an entry in the count it stays at zero`() {
        val button = Button(ButtonAction.App("com.chat", "com.chat.Main"))
        assertEquals(0, TileNotifications.badgeFor(button, emptyMap(), system))
    }

    @Test
    fun `a tile without a watched package never counts`() {
        val button = Button(ButtonAction.Action(Builtin.CAMERA))
        assertEquals(0, TileNotifications.badgeFor(button, mapOf("com.sms.app" to 3), system))
    }

    /**
     * eleven unseen missed calls in the list and nothing on the tile: the notice about a
     * missed call comes from the system (telecom), not from the phone app. so the tile
     * counts the call log, which is what the user means.
     */
    @Test
    fun `the missed calls tile counts the call log`() {
        val tile = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = true)
        assertEquals(11, TileNotifications.badgeFor(tile, emptyMap(), system, missed = 11))
    }

    @Test
    fun `foreign notices do not count there`() {
        // or the number would stand twice as soon as the phone app reports as well.
        val tile = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = true)
        assertEquals(
            3,
            TileNotifications.badgeFor(tile, mapOf("com.dialer.app" to 7), system, missed = 3),
        )
    }

    /** a blinking phone tile can only mean one thing anyway: you missed a call. */
    @Test
    fun `the phone tile counts missed calls, not our own notices`() {
        val tile = Button(action = ButtonAction.Action(Builtin.DIALER), blink = true)
        assertEquals(
            2,
            TileNotifications.badgeFor(
                tile,
                mapOf("org.biglau" to 5),
                SystemPackages(sms = "org.biglau", dialer = "org.biglau"),
                missed = 2,
            ),
        )
    }

    @Test
    fun `without missed calls the phone tile stays quiet`() {
        val tile = Button(action = ButtonAction.Action(Builtin.DIALER), blink = true)
        assertEquals(
            0,
            TileNotifications.badgeFor(
                tile,
                mapOf("org.biglau" to 5),
                SystemPackages(sms = "org.biglau", dialer = "org.biglau"),
                missed = 0,
            ),
        )
    }

    @Test
    fun `without blinking the tile stays quiet on missed calls too`() {
        val tile = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = false)
        assertEquals(0, TileNotifications.badgeFor(tile, emptyMap(), system, missed = 11))
    }

    /**
     * counting **notices** of the default sms app needs access to foreign notices - a
     * permission nobody grants by themselves - and hangs on our own notice as soon as
     * BigLau is the default app.
     */
    @Test
    fun `the messages tile counts unread messages`() {
        val tile = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            3,
            TileNotifications.badgeFor(tile, mapOf("com.sms.app" to 9), system, unread = 3),
        )
    }

    @Test
    fun `without read permission the notices remain the answer`() {
        // null does not mean "none unread" but "we may not look".
        val tile = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            9,
            TileNotifications.badgeFor(tile, mapOf("com.sms.app" to 9), system, unread = null),
        )
    }

    @Test
    fun `zero unread is a statement`() {
        // whoever has read everything sees no number, even while the other app's notice
        // still stands.
        val tile = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            0,
            TileNotifications.badgeFor(tile, mapOf("com.sms.app" to 9), system, unread = 0),
        )
    }

    @Test
    fun `other tiles stay with the notices`() {
        val tile = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            4,
            TileNotifications.badgeFor(tile, mapOf("com.sms.app" to 4), system, missed = 11),
        )
    }
}

/**
 * where a blink switch makes sense at all.
 *
 * `Button.blink` was honoured from the start but reachable nowhere in the editor. offering
 * the switch for a clock would promise something that never happens.
 */
class CanBlinkTest {

    @Test
    fun `something can arrive behind an app`() {
        assertTrue(TileNotifications.canBlink(ButtonAction.App("com.beispiel", "com.beispiel.Main")))
    }

    @Test
    fun `phone and messages too`() {
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.DIALER)))
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.MESSAGES)))
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.MISSED_CALLS)))
    }

    @Test
    fun `a clock does not notify`() {
        assertFalse(TileNotifications.canBlink(ButtonAction.Action(Builtin.CLOCK)))
        assertFalse(TileNotifications.canBlink(ButtonAction.Action(Builtin.BATTERY)))
    }

    @Test
    fun `an empty tile even less so`() {
        assertFalse(TileNotifications.canBlink(ButtonAction.None))
    }

    @Test
    fun `a folder and a link neither`() {
        // a folder could one day add up its contents; today it does not, and a switch for it
        // would be a promise without cover.
        assertFalse(TileNotifications.canBlink(ButtonAction.Folder("f1")))
        assertFalse(TileNotifications.canBlink(ButtonAction.Link("https://orf.at")))
    }
}
