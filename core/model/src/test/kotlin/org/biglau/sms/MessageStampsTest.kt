package org.biglau.sms

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wann eine Nachricht kam — in der Unterhaltung stand bisher gar keine Zeit.
 *
 * Wer eine Nachricht liest, will wissen, ob sie von eben ist oder von letzter Woche. Bei
 * „bin unterwegs" ist das der ganze Unterschied.
 */
class MessageStampsTest {

    private fun zeit(tag: Int, stunde: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, tag, stunde, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Die erste Nachricht ueberhaupt bekommt immer eine Tagesueberschrift - sonst haette
    // die oberste Nachricht als einzige keine Einordnung.
    @Test
    fun `die erste nachricht bekommt einen tag`() {
        assertEquals(true, MessageStamps.startsNewDay(null, zeit(2, 10)))
    }

    @Test
    fun `am selben tag kommt keine zweite ueberschrift`() {
        assertEquals(false, MessageStamps.startsNewDay(zeit(2, 9), zeit(2, 23)))
    }

    @Test
    fun `ein neuer tag bekommt eine ueberschrift`() {
        assertEquals(true, MessageStamps.startsNewDay(zeit(1, 23), zeit(2, 0)))
    }

    // Ueber Mitternacht liegen nur Minuten und trotzdem zwei Tage - genau der Fall, den
    // eine Differenz in Stunden falsch machen wuerde.
    @Test
    fun `wenige minuten ueber mitternacht sind zwei tage`() {
        val vorMitternacht = zeit(1, 23) + 59 * 60_000
        val nachMitternacht = zeit(2, 0) + 60_000
        assertEquals(false, MessageStamps.sameDay(vorMitternacht, nachMitternacht))
        assertEquals(true, MessageStamps.startsNewDay(vorMitternacht, nachMitternacht))
    }

    // Und derselbe Tag im anderen Jahr ist nicht derselbe Tag.
    @Test
    fun `derselbe tag im anderen jahr zaehlt nicht als gleich`() {
        val jetzt = zeit(2, 12)
        val vorEinemJahr = Calendar.getInstance().apply {
            timeInMillis = jetzt
            add(Calendar.YEAR, -1)
        }.timeInMillis
        assertEquals(false, MessageStamps.sameDay(vorEinemJahr, jetzt))
    }
}
