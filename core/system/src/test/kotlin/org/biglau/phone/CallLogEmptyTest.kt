package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallLogEmptyTest {

    private var naechsteId = 0L

    private fun anruf(art: CallDirection, nummer: String = "+4366411111") = CallEntry(
        id = ++naechsteId,
        number = nummer,
        name = null,
        direction = art,
        timestamp = 1_000_000L - naechsteId,
        durationSeconds = 0,
    )

    private fun gruppen(vararg anrufe: CallEntry) = CallLogGrouping.group(anrufe.toList())

    private val alleArten = CallDirection.entries.toSet()

    @Test
    fun `mit sichtbaren Anrufen gibt es keinen Grund`() {
        assertNull(
            CallLogEmpty.reason(gruppen(anruf(CallDirection.INCOMING)), false, alleArten),
        )
    }

    @Test
    fun `ohne jeden Anruf ist die Liste wirklich leer`() {
        assertEquals(
            EmptyCallLog.NO_CALLS,
            CallLogEmpty.reason(emptyList(), false, alleArten),
        )
    }

    @Test
    fun `sind alle Arten ausgeblendet, sagt die Liste das`() {
        // Genau der Fall vom Emulator: sieben Anrufe im Protokoll, jede Art abgewaehlt,
        // und auf dem Bildschirm stand "Noch keine Anrufe".
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(
                gruppen(anruf(CallDirection.INCOMING), anruf(CallDirection.MISSED, "+4366422222")),
                false,
                emptySet(),
            ),
        )
    }

    @Test
    fun `auch eine einzelne ausgeblendete Art kann die Liste leeren`() {
        // Es muessen nicht alle Arten abgewaehlt sein - es reicht, dass die vorhandenen
        // Anrufe alle von einer abgewaehlten sind.
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(
                gruppen(anruf(CallDirection.BLOCKED)),
                false,
                alleArten - CallDirection.BLOCKED,
            ),
        )
    }

    @Test
    fun `ohne verpasste Anrufe liegt es am Filter darueber`() {
        assertEquals(
            EmptyCallLog.NO_MISSED,
            CallLogEmpty.reason(gruppen(anruf(CallDirection.OUTGOING)), true, alleArten),
        )
    }

    @Test
    fun `die ausgeblendete Art wiegt schwerer als der Filter`() {
        // Beides trifft zu. Genannt wird der Grund, den man nicht sehen kann: der Filter
        // "nur verpasste" steht als Knopf ueber der Liste, die Artenwahl in den
        // Einstellungen.
        assertEquals(
            EmptyCallLog.HIDDEN_BY_TYPE,
            CallLogEmpty.reason(gruppen(anruf(CallDirection.OUTGOING)), true, emptySet()),
        )
    }

    @Test
    fun `ein verpasster Anruf bleibt bei eingeschaltetem Filter sichtbar`() {
        assertNull(
            CallLogEmpty.reason(gruppen(anruf(CallDirection.MISSED)), true, alleArten),
        )
    }
}
