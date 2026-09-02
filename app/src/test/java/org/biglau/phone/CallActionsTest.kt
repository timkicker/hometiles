package org.biglau.phone

import org.biglau.data.AudioRoute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallActionsTest {

    private fun view(
        status: CallStatus,
        started: Long? = null,
        muted: Boolean = false,
        speaker: Boolean = false,
        bluetooth: Boolean = false,
        anderer: String? = null,
        andererGehalten: Boolean = false,
        name: String? = null,
        number: String = "+436601234567",
    ) = CallView(
        status = status,
        number = number,
        name = name,
        startedAtMillis = started,
        muted = muted,
        audioRoute = if (speaker) AudioRoute.SPEAKER else AudioRoute.EARPIECE,
        bluetoothAvailable = bluetooth,
        otherName = anderer,
        otherHeld = andererGehalten,
    )

    @Test
    fun `ein klingelnder Anruf bietet Annehmen und Ablehnen`() {
        val actions = CallActions.availableFor(view(CallStatus.RINGING))
        assertEquals(listOf(CallAction.ANSWER, CallAction.REJECT), actions)
    }

    @Test
    fun `ein klingelnder Anruf zeigt nie Auflegen`() {
        // "Auflegen" auf einem klingelnden Anruf liest sich wie "Ablehnen" - und wer das
        // verwechselt, hat den Anruf verloren.
        assertTrue(CallAction.HANG_UP !in CallActions.availableFor(view(CallStatus.RINGING)))
    }

    @Test
    fun `ein laufendes Gespraech bietet Auflegen aber kein Annehmen`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.HANG_UP in actions)
        assertTrue(CallAction.ANSWER !in actions)
    }

    @Test
    fun `stumm und laut wechseln sich ab`() {
        assertTrue(CallAction.MUTE in CallActions.availableFor(view(CallStatus.ACTIVE, muted = false)))
        assertTrue(CallAction.UNMUTE in CallActions.availableFor(view(CallStatus.ACTIVE, muted = true)))
    }

    @Test
    fun `ein gehaltener Anruf bietet Fortsetzen`() {
        val actions = CallActions.availableFor(view(CallStatus.HOLDING))
        assertTrue(CallAction.UNHOLD in actions)
        assertTrue(CallAction.HOLD !in actions)
    }

    @Test
    fun `ein beendeter Anruf bietet nichts mehr`() {
        listOf(CallStatus.DISCONNECTED, CallStatus.DISCONNECTING, CallStatus.OTHER).forEach {
            assertTrue(CallActions.availableFor(view(it)).isEmpty())
        }
    }

    @Test
    fun `die Dauer laeuft erst ab dem Verbinden`() {
        assertNull(CallActions.durationSeconds(view(CallStatus.RINGING, started = 1000L), 5000L))
        assertNull(CallActions.durationSeconds(view(CallStatus.DIALING, started = 1000L), 5000L))
        assertEquals(4L, CallActions.durationSeconds(view(CallStatus.ACTIVE, started = 1000L), 5000L))
    }

    @Test
    fun `ohne Startzeit gibt es keine Dauer`() {
        assertNull(CallActions.durationSeconds(view(CallStatus.ACTIVE, started = null), 5000L))
    }

    @Test
    fun `eine zurueckspringende Uhr ergibt keine negative Dauer`() {
        assertEquals(0L, CallActions.durationSeconds(view(CallStatus.ACTIVE, started = 9000L), 1000L))
    }

    @Test
    fun `die Dauer wird als Minuten und Sekunden geschrieben`() {
        assertEquals("0:00", CallActions.formatDuration(0))
        assertEquals("0:07", CallActions.formatDuration(7))
        assertEquals("1:05", CallActions.formatDuration(65))
        assertEquals("59:59", CallActions.formatDuration(3599))
    }

    @Test
    fun `ab einer Stunde kommen Stunden dazu`() {
        assertEquals("1:00:00", CallActions.formatDuration(3600))
        assertEquals("2:03:04", CallActions.formatDuration(7384))
    }

    @Test
    fun `die Ueberschrift nimmt den Namen wenn es einen gibt`() {
        assertEquals("Oma", CallActions.headline(view(CallStatus.RINGING, name = "Oma"), "Unbekannt"))
    }

    @Test
    fun `ohne Namen steht die lesbar gruppierte Nummer da`() {
        assertEquals("+436 601 234 567", CallActions.headline(view(CallStatus.RINGING, name = null), "Unbekannt"))
    }

    @Test
    fun `eine unterdrueckte Nummer ergibt ein Fragezeichen statt einer leeren Zeile`() {
        assertEquals("Unbekannt", CallActions.headline(view(CallStatus.RINGING, name = null, number = ""), "Unbekannt"))
        assertEquals("Unbekannt", CallActions.headline(view(CallStatus.RINGING, name = "  ", number = ""), "Unbekannt"))
    }

    // --- Die geoeffnete Tastatur (PLAN.md 4.6) ---

    /**
     * Der Fund vom 02.09.2026: mit allen fuenf Zeilen blieb fuer die Tastatur ein Streifen
     * von zwoelf Bildpunkten - die Ziffern wurden nicht einmal mehr gezeichnet.
     */
    @Test
    fun `neben der Tastatur bleiben nur zwei Zeilen`() {
        val actions = CallActions.whileKeypad(view(CallStatus.ACTIVE))
        assertEquals(listOf(CallAction.HANG_UP, CallAction.KEYPAD), actions)
    }

    @Test
    fun `neben der Tastatur bleibt der Weg zurueck sichtbar`() {
        // Vorher schloss sie nur die Ruecktaste - das ahnt niemand.
        assertTrue(CallAction.KEYPAD in CallActions.whileKeypad(view(CallStatus.ACTIVE)))
    }

    @Test
    fun `neben der Tastatur bleibt Auflegen`() {
        assertTrue(CallAction.HANG_UP in CallActions.whileKeypad(view(CallStatus.ACTIVE)))
    }

    @Test
    fun `die Auswahl ist immer eine Teilmenge der moeglichen Zeilen`() {
        CallStatus.entries.forEach { status ->
            val alle = CallActions.availableFor(view(status))
            assertTrue(status.name, CallActions.whileKeypad(view(status)).all { it in alle })
        }
    }

    // --- Der Lautsprecher (PLAN.md 4.6) ---

    /**
     * Der Fund vom 02.09.2026: es gab nur „Lautsprecher", und der schaltete ihn *ein*. Ein
     * zweiter Druck tat dasselbe noch einmal. Wer ihn versehentlich anschaltete, bekam ihn
     * bis zum Auflegen nicht mehr weg - das Gespräch lief derweil laut durch den Raum. Im
     * Quelltext stand die Absicht sogar schon da, aber als tote Zeile:
     * `if (view.speakerOn) SPEAKER else SPEAKER`.
     */
    @Test
    fun `bei laufendem Lautsprecher steht dort das Ausschalten`() {
        val an = CallActions.availableFor(view(CallStatus.ACTIVE, speaker = true))
        assertTrue(CallAction.SPEAKER_OFF in an)
        assertTrue(CallAction.SPEAKER !in an)
    }

    @Test
    fun `ohne Lautsprecher steht dort das Einschalten`() {
        val aus = CallActions.availableFor(view(CallStatus.ACTIVE, speaker = false))
        assertTrue(CallAction.SPEAKER in aus)
        assertTrue(CallAction.SPEAKER_OFF !in aus)
    }

    @Test
    fun `auch beim Waehlen laesst sich der Lautsprecher wieder ausschalten`() {
        val an = CallActions.availableFor(view(CallStatus.DIALING, speaker = true))
        assertTrue(CallAction.SPEAKER_OFF in an)
    }

    @Test
    fun `die Zahl der Zeilen aendert sich durch den Lautsprecher nicht`() {
        // Sonst huepfte die ganze Knopfreihe beim Umschalten, und der Finger traefe
        // beim zweiten Druck etwas anderes.
        CallStatus.entries.forEach { status ->
            assertEquals(
                status.name,
                CallActions.availableFor(view(status, speaker = false)).size,
                CallActions.availableFor(view(status, speaker = true)).size,
            )
        }
    }

    // --- Der zweite Anruf (PLAN.md 4.6) ---

    /**
     * Der Fund vom 02.09.2026: `otherCallWaiting` wurde gesetzt und **nirgends gelesen**.
     * Am Emulator nachgestellt - waehrend eines Gespraechs mit „Anna" rief „Bernd" an: der
     * Bildschirm zeigte nur noch Bernd, und nach dem Annehmen war Anna weder zu sehen noch
     * zu erreichen. Kein Knopf fuehrte zu ihr zurueck.
     */
    @Test
    fun `mit gehaltenem zweiten Anruf steht dort Wechseln statt Halten`() {
        val actions = CallActions.availableFor(
            view(CallStatus.ACTIVE, anderer = "Anna Bauer", andererGehalten = true),
        )
        assertTrue(CallAction.SWITCH in actions)
        assertTrue(CallAction.HOLD !in actions)
    }

    @Test
    fun `ohne zweiten Anruf bleibt es beim Halten`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.HOLD in actions)
        assertTrue(CallAction.SWITCH !in actions)
    }

    @Test
    fun `ein klingelnder zweiter Anruf ersetzt das Halten noch nicht`() {
        // Solange er klingelt, ist er nicht gehalten - da gibt es nichts zu wechseln,
        // sondern anzunehmen oder abzulehnen.
        val actions = CallActions.availableFor(
            view(CallStatus.ACTIVE, anderer = "Bernd", andererGehalten = false),
        )
        assertTrue(CallAction.HOLD in actions)
    }

    // --- Wer im Vordergrund steht ---

    @Test
    fun `es klingelt - dann steht der klingelnde vorn`() {
        val index = CallForeground.pick(listOf(CallStatus.ACTIVE, CallStatus.RINGING))
        assertEquals(1, index)
    }

    @Test
    fun `sonst der laufende vor dem gehaltenen`() {
        assertEquals(1, CallForeground.pick(listOf(CallStatus.HOLDING, CallStatus.ACTIVE)))
    }

    @Test
    fun `bleibt nur ein gehaltener, steht der vorn`() {
        assertEquals(0, CallForeground.pick(listOf(CallStatus.HOLDING)))
    }

    @Test
    fun `ohne Anruf gibt es keinen Vordergrund`() {
        assertNull(CallForeground.pick(emptyList()))
    }

    // --- Bluetooth (PLAN.md P5) ---

    /**
     * `PLAN.md` P5 nennt die „Bluetooth-Umschaltung"; gebaut war sie nicht, und eine
     * sechste Knopfzeile passt auf drei Zoll auch nicht mehr - fuenf fuellen den Bildschirm
     * bereits. Deshalb wird aus dem Umschalter eine Auswahl, **sobald** ein Geraet da ist:
     * die Zeile sagt, wohin der Ton geht, und fuehrt zu den drei Wegen.
     */
    @Test
    fun `mit Bluetooth wird aus dem Umschalter eine Auswahl`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE, bluetooth = true))
        assertTrue(CallAction.AUDIO in actions)
        assertTrue(CallAction.SPEAKER !in actions)
        assertTrue(CallAction.SPEAKER_OFF !in actions)
    }

    @Test
    fun `ohne Bluetooth bleibt der Umschalter`() {
        val actions = CallActions.availableFor(view(CallStatus.ACTIVE))
        assertTrue(CallAction.SPEAKER in actions)
        assertTrue(CallAction.AUDIO !in actions)
    }

    @Test
    fun `die Zahl der Zeilen aendert sich durch Bluetooth nicht`() {
        CallStatus.entries.forEach { status ->
            assertEquals(
                status.name,
                CallActions.availableFor(view(status)).size,
                CallActions.availableFor(view(status, bluetooth = true)).size,
            )
        }
    }
}
