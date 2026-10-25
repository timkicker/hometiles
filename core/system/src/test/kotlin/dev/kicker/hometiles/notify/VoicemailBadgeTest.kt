package dev.kicker.hometiles.notify

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * waiting voicemail shows on the phone tile (issue #2).
 *
 * the phone service posts one notice on its channel `voiceMail` while the network says a
 * message waits, and takes it away when it no longer does (emulator, voicemail indicator on
 * and off, 25.10.2026). the same package posts other notices too, so the channel counts.
 */
class VoicemailBadgeTest {

    private fun row(channel: String?) = NotificationRow(
        packageName = "com.android.phone",
        clearable = true,
        ongoing = false,
        groupSummary = false,
        channel = channel,
    )

    @Test
    fun `the voicemail notice counts by its channel`() {
        assertEquals(1, NotificationCounts.voicemail(listOf(row("voiceMail"), row("callForward"), row(null))))
        assertEquals(0, NotificationCounts.voicemail(listOf(row("callForward"))))
    }

    @Test
    fun `the phone tile adds voicemail to the missed calls`() {
        val system = SystemPackages(sms = null, dialer = null)
        val tile = Button(action = ButtonAction.Action(Builtin.DIALER), blink = true)
        assertEquals(3, TileNotifications.badgeFor(tile, emptyMap(), system, missed = 2, voicemail = 1))
        assertEquals(1, TileNotifications.badgeFor(tile, emptyMap(), system, voicemail = 1))
    }
}
