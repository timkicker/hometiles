package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallActionsTest {

    private fun view(
        status: CallStatus,
        started: Long? = null,
        muted: Boolean = false,
        name: String? = null,
        number: String = "+436601234567",
    ) = CallView(status, number, name, started, muted)

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
        assertTrue(!CallActions.showsAnswer(view(CallStatus.ACTIVE)))
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
        assertEquals("Oma", CallActions.headline(view(CallStatus.RINGING, name = "Oma")))
    }

    @Test
    fun `ohne Namen steht die lesbar gruppierte Nummer da`() {
        assertEquals("+436 601 234 567", CallActions.headline(view(CallStatus.RINGING, name = null)))
    }

    @Test
    fun `eine unterdrueckte Nummer ergibt ein Fragezeichen statt einer leeren Zeile`() {
        assertEquals("?", CallActions.headline(view(CallStatus.RINGING, name = null, number = "")))
        assertEquals("?", CallActions.headline(view(CallStatus.RINGING, name = "  ", number = "")))
    }
}
