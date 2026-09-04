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

    /**
     * Die Telefon-Kachel sieht auf **gar keine** fremden Meldungen mehr.
     *
     * Hier stand bis zum 04.09.2026 das Gegenteil: sie folge dem eingestellten
     * Standard-Dialer. Das war richtig, solange das eine fremde App war - deren Meldung
     * ueber einen verpassten Anruf war genau das, was die Kachel meinen sollte.
     *
     * Sobald BigLau die Rolle haelt, ist der Standard-Dialer BigLau selbst. Und BigLau
     * meldet **Nachrichten**: die Telefon-Kachel haette geblinkt, wenn eine SMS ankommt.
     * Dieselbe Falle wie bei den verpassten Anrufen, nur einen Tag spaeter gesehen - und
     * dieser Test hielt sie fest, statt sie zu finden.
     */
    @Test
    fun `die Telefon-Kachel folgt keiner fremden App mehr`() {
        val action = ButtonAction.Action(Builtin.DIALER)
        assertNull(TileNotifications.watchedPackage(action, system))
    }

    @Test
    fun `ohne gesetzte Standard-App wird nichts beobachtet`() {
        assertNull(TileNotifications.watchedPackage(ButtonAction.Action(Builtin.MESSAGES), SystemPackages()))
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

    // --- Verpasste Anrufe zaehlen anders (02.09.2026) ---

    /**
     * Der Fund am Emulator: **elf ungesehene verpasste Anrufe** in der Liste, und auf der
     * Kachel stand nichts. Der Grund: die Kachel sah auf die Meldungen der
     * Standard-Telefon-App — und das ist BigLau selbst, sobald sie die Rolle hat. Die
     * Meldung ueber einen verpassten Anruf kommt aber vom System (Telecom), nicht von der
     * Telefon-App. Jetzt zaehlt die Kachel die Anrufliste, also das, was der Nutzer meint.
     */
    @Test
    fun `die Kachel fuer verpasste Anrufe zaehlt die Anrufliste`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = true)
        assertEquals(11, TileNotifications.badgeFor(kachel, emptyMap(), system, missed = 11))
    }

    @Test
    fun `fremde Meldungen zaehlen dort nicht mit`() {
        // Sonst stuende die Zahl doppelt da, sobald die Telefon-App selbst meldet.
        val kachel = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = true)
        assertEquals(
            3,
            TileNotifications.badgeFor(kachel, mapOf("com.dialer.app" to 7), system, missed = 3),
        )
    }


    /**
     * Und die **Telefon**-Kachel genauso — dieselbe Falle, nur spaeter gesehen.
     *
     * Sie zaehlte die Meldungen der Standard-Telefon-App. Seit BigLau die Rolle haelt
     * (04.09.2026 auf dem Geraet des Nutzers), ist das BigLau selbst — und BigLau meldet
     * **Nachrichten**. Die Telefon-Kachel haette geblinkt, wenn eine SMS ankommt.
     *
     * Was eine blinkende Telefon-Kachel heissen soll, ist ohnehin nur eines: du hast einen
     * Anruf verpasst.
     */
    @Test
    fun `die Telefon-Kachel zaehlt verpasste Anrufe, nicht eigene Meldungen`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.DIALER), blink = true)
        assertEquals(
            2,
            TileNotifications.badgeFor(
                kachel,
                mapOf("org.biglau" to 5),
                SystemPackages(sms = "org.biglau", dialer = "org.biglau"),
                missed = 2,
            ),
        )
    }

    @Test
    fun `ohne verpasste Anrufe bleibt die Telefon-Kachel still`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.DIALER), blink = true)
        assertEquals(
            0,
            TileNotifications.badgeFor(
                kachel,
                mapOf("org.biglau" to 5),
                SystemPackages(sms = "org.biglau", dialer = "org.biglau"),
                missed = 0,
            ),
        )
    }

    @Test
    fun `ohne Blinken bleibt die Kachel auch bei verpassten Anrufen still`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.MISSED_CALLS), blink = false)
        assertEquals(0, TileNotifications.badgeFor(kachel, emptyMap(), system, missed = 11))
    }


    /**
     * Dieselbe Frage bei den Nachrichten: die Kachel zaehlte **Meldungen** der
     * Standard-SMS-App. Das braucht den Zugriff auf fremde Meldungen — eine eigene
     * Erlaubnis, die niemand von selbst erteilt — und haengt, sobald BigLau selbst die
     * Standard-App ist, an der eigenen Meldung statt an dem, was ungelesen ist.
     */
    @Test
    fun `die Nachrichten-Kachel zaehlt ungelesene Nachrichten`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            3,
            TileNotifications.badgeFor(kachel, mapOf("com.sms.app" to 9), system, unread = 3),
        )
    }

    @Test
    fun `ohne Leseerlaubnis bleiben die Meldungen die Auskunft`() {
        // Null heisst nicht "keine ungelesenen", sondern "wir duerfen nicht nachsehen".
        val kachel = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            9,
            TileNotifications.badgeFor(kachel, mapOf("com.sms.app" to 9), system, unread = null),
        )
    }

    @Test
    fun `null ungelesene sind eine Aussage`() {
        // Wer alles gelesen hat, soll keine Zahl mehr sehen - auch wenn die Meldung der
        // anderen App noch steht.
        val kachel = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            0,
            TileNotifications.badgeFor(kachel, mapOf("com.sms.app" to 9), system, unread = 0),
        )
    }

    @Test
    fun `andere Kacheln bleiben bei den Meldungen`() {
        val kachel = Button(action = ButtonAction.Action(Builtin.MESSAGES), blink = true)
        assertEquals(
            4,
            TileNotifications.badgeFor(kachel, mapOf("com.sms.app" to 4), system, missed = 11),
        )
    }
}

/**
 * Wo ein Blink-Schalter überhaupt Sinn ergibt.
 *
 * Das Feld `Button.blink` wurde von Anfang an beachtet, war aber im Editor nirgends
 * erreichbar - man konnte es nur über eine importierte Datei ändern. Beim Nachrüsten stellt
 * sich die Frage, wo der Schalter erscheint: einen für eine Uhr anzubieten hieße, etwas zu
 * versprechen, das nie eintritt.
 */
class CanBlinkTest {

    @Test
    fun `hinter einer App kann etwas ankommen`() {
        assertTrue(TileNotifications.canBlink(ButtonAction.App("com.beispiel", "com.beispiel.Main")))
    }

    @Test
    fun `Telefon und Nachrichten auch`() {
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.DIALER)))
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.MESSAGES)))
        assertTrue(TileNotifications.canBlink(ButtonAction.Action(Builtin.MISSED_CALLS)))
    }

    @Test
    fun `eine Uhr benachrichtigt nicht`() {
        assertFalse(TileNotifications.canBlink(ButtonAction.Action(Builtin.CLOCK)))
        assertFalse(TileNotifications.canBlink(ButtonAction.Action(Builtin.BATTERY)))
    }

    @Test
    fun `eine leere Kachel erst recht nicht`() {
        assertFalse(TileNotifications.canBlink(ButtonAction.None))
    }

    @Test
    fun `ein Ordner und ein Link auch nicht`() {
        // Der Ordner koennte irgendwann den Inhalt zusammenzaehlen; heute tut er es nicht,
        // und ein Schalter dafuer waere eine Zusage ohne Deckung.
        assertFalse(TileNotifications.canBlink(ButtonAction.Folder("f1")))
        assertFalse(TileNotifications.canBlink(ButtonAction.Link("https://orf.at")))
    }

}
