package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLogGroupingTest {

    private var nextId = 1L
    private fun call(
        number: String,
        at: Long,
        direction: CallDirection = CallDirection.OUTGOING,
        name: String? = null,
    ) = CallEntry(nextId++, number, name, direction, at, 0)

    @Test
    fun `aufeinanderfolgende Anrufe derselben Nummer werden zusammengefasst`() {
        // Ohne das steht ein dreimal versuchter Anruf dreimal in der Liste.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200), call("+43660", 100)),
        )
        assertEquals(1, groups.size)
        assertEquals(3, groups.first().count)
    }

    @Test
    fun `verschiedene Nummern bleiben getrennt`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertEquals(2, groups.size)
    }

    @Test
    fun `dieselbe Nummer mit anderer Schreibweise zaehlt als dieselbe`() {
        val groups = CallLogGrouping.group(listOf(call("+43 660 123", 300), call("+43660123", 200)))
        assertEquals(1, groups.size)
    }

    @Test
    fun `nicht aufeinanderfolgende Anrufe bleiben getrennt`() {
        // Sonst verschoebe sich die zeitliche Reihenfolge und "zuletzt angerufen"
        // waere keine verlaessliche Aussage mehr.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43512", 200), call("+43660", 100)),
        )
        assertEquals(3, groups.size)
        assertEquals(listOf("+43660", "+43512", "+43660"), groups.map { it.number })
    }

    @Test
    fun `die neueste Zeile steht vorn`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 100), call("+43660", 300), call("+43660", 200)))
        assertEquals(300L, groups.first().latest.timestamp)
    }

    @Test
    fun `der Name kommt aus der ersten Zeile die einen hat`() {
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300, name = null), call("+43660", 200, name = "Oma")),
        )
        assertEquals("Oma", groups.first().name)
    }

    @Test
    fun `eine Gruppe mit verpasstem Anruf wird als solche erkannt`() {
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200, direction = CallDirection.MISSED)),
        )
        assertTrue(groups.first().hasMissed)
        assertEquals(1, CallLogGrouping.onlyMissed(groups).size)
    }

    @Test
    fun `ohne verpasste Anrufe bleibt die gefilterte Liste leer`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertTrue(CallLogGrouping.onlyMissed(groups).isEmpty())
    }

    @Test
    fun `unterdrueckte Nummern werden nicht zusammengefasst`() {
        // Sie kommen ohne Ziffern an. Wuerde man danach gruppieren, erschienen drei
        // verschiedene anonyme Anrufer als ein einziger, dreimal anrufender Mensch.
        val groups = CallLogGrouping.group(
            listOf(call("", 300), call("", 200), call("unbekannt", 100)),
        )
        assertEquals(3, groups.size)
    }

    @Test
    fun `beim Loeschen einer Zeile gehen alle darin gebuendelten Anrufe mit`() {
        // Sonst taucht die Zeile nach dem Neuladen mit einem Eintrag weniger wieder auf,
        // und der Nutzer haelt das Loeschen fuer kaputt.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200), call("+43660", 100)),
        )
        assertEquals(3, CallLogGrouping.idsOf(groups.first()).size)
    }

    @Test
    fun `die Kennungen mehrerer Gruppen kommen zusammen`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertEquals(2, CallLogGrouping.idsOf(groups).size)
    }

    @Test
    fun `gefiltert wird nach erlaubten Anrufarten`() {
        val groups = CallLogGrouping.group(
            listOf(
                call("+43660", 300, direction = CallDirection.MISSED),
                call("+43512", 200, direction = CallDirection.OUTGOING),
            ),
        )
        assertEquals(
            listOf("+43660"),
            CallLogGrouping.visible(groups, setOf(CallDirection.MISSED)).map { it.number },
        )
    }

    @Test
    fun `eine Gruppe bleibt sichtbar wenn einer ihrer Anrufe passt`() {
        val groups = CallLogGrouping.group(
            listOf(
                call("+43660", 300, direction = CallDirection.OUTGOING),
                call("+43660", 200, direction = CallDirection.MISSED),
            ),
        )
        assertEquals(1, CallLogGrouping.visible(groups, setOf(CallDirection.MISSED)).size)
    }

    @Test
    fun `ohne erlaubte Art bleibt nichts sichtbar`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300)))
        assertTrue(CallLogGrouping.visible(groups, emptySet()).isEmpty())
    }

    @Test
    fun `eine leere Liste ergibt keine Gruppen`() {
        assertTrue(CallLogGrouping.group(emptyList()).isEmpty())
    }

    @Test
    fun `unsortierte Eingabe wird zuerst sortiert`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 100), call("+43512", 300)))
        assertEquals(listOf("+43512", "+43660"), groups.map { it.number })
    }
}
