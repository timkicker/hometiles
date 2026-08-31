package org.biglau.notify

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileNotificationsTest {

    private val system = SystemPackages(sms = "com.sms.app", dialer = "com.dialer.app")

    @Test
    fun `eine App-Kachel beobachtet ihre eigene App`() {
        val action = ButtonAction.App("com.chat", "com.chat.Main")
        assertEquals("com.chat", TileNotifications.watchedPackage(action, system))
    }

    @Test
    fun `die Nachrichten-Kachel folgt der eingestellten Standard-App`() {
        // Wechselt der Nutzer seine SMS-App, blinkt die Kachel weiter richtig,
        // ohne dass er etwas umstellen muss.
        val action = ButtonAction.Action(Builtin.MESSAGES)
        assertEquals("com.sms.app", TileNotifications.watchedPackage(action, system))
        assertEquals("andere.sms", TileNotifications.watchedPackage(action, SystemPackages(sms = "andere.sms")))
    }

    @Test
    fun `die Telefon-Kachel folgt dem eingestellten Standard-Dialer`() {
        val action = ButtonAction.Action(Builtin.DIALER)
        assertEquals("com.dialer.app", TileNotifications.watchedPackage(action, system))
    }

    @Test
    fun `ohne gesetzte Standard-App wird nichts beobachtet`() {
        assertNull(TileNotifications.watchedPackage(ButtonAction.Action(Builtin.MESSAGES), SystemPackages()))
        assertNull(TileNotifications.watchedPackage(ButtonAction.Action(Builtin.DIALER), SystemPackages()))
    }

    @Test
    fun `andere eingebaute Aktionen beobachten nichts`() {
        listOf(Builtin.CAMERA, Builtin.SETTINGS, Builtin.APP_LIST, Builtin.SOS).forEach { builtin ->
            assertNull(TileNotifications.watchedPackage(ButtonAction.Action(builtin), system))
        }
    }

    @Test
    fun `Kontakt- und Screen-Kacheln beobachten nichts`() {
        assertNull(TileNotifications.watchedPackage(ButtonAction.Contact("Anna", "+431"), system))
        assertNull(TileNotifications.watchedPackage(ButtonAction.GoToScreen("s2"), system))
        assertNull(TileNotifications.watchedPackage(ButtonAction.None, system))
    }

    @Test
    fun `der Zaehler kommt aus dem beobachteten Paket`() {
        val button = Button(ButtonAction.Action(Builtin.MESSAGES))
        val counts = mapOf("com.sms.app" to 3, "com.chat" to 9)
        assertEquals(3, TileNotifications.badgeFor(button, counts, system))
    }

    @Test
    fun `eine Kachel mit abgeschaltetem Blinken zaehlt nicht`() {
        val button = Button(ButtonAction.Action(Builtin.MESSAGES), blink = false)
        assertEquals(0, TileNotifications.badgeFor(button, mapOf("com.sms.app" to 3), system))
    }

    @Test
    fun `ohne Eintrag im Zaehler bleibt es bei null`() {
        val button = Button(ButtonAction.App("com.chat", "com.chat.Main"))
        assertEquals(0, TileNotifications.badgeFor(button, emptyMap(), system))
    }

    @Test
    fun `eine Kachel ohne beobachtetes Paket zaehlt nie`() {
        val button = Button(ButtonAction.Action(Builtin.CAMERA))
        assertEquals(0, TileNotifications.badgeFor(button, mapOf("com.sms.app" to 3), system))
    }
}
