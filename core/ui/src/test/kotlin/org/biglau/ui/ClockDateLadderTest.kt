package org.biglau.ui

import java.text.SimpleDateFormat
import java.util.Locale
import org.biglau.data.ClockDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Erst kuerzen, dann umbrechen - auch beim Datum auf der Uhr-Kachel.
 *
 * Am Telefon des Nutzers gesehen: auf der 1x1-Kachel stand „Wednesday, September" und
 * darunter die **2 allein**. Im Quelltext stand dazu „Auf der Kachel ist Platz fuer die
 * langen Namen" - eine Behauptung ueber ein Geraet, die niemand nachgemessen hatte.
 *
 * Diese Regel prueft die Stufenleiter und das, womit gemessen wird. Ob eine Stufe passt,
 * entscheidet die Messung in `ClockContent`; was hier festgehalten wird, ist, dass es
 * ueberhaupt eine kuerzere Stufe *gibt* und dass sie kuerzer ist.
 */
class ClockDateLadderTest {

    @Test
    fun `die Kachel hat eine kuerzere Stufe als die lange Form`() {
        val stufen = ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE_WEEKDAY, onTile = true)
        assertTrue("keine Stufe zum Kuerzen: $stufen", stufen.size >= 2)
        assertEquals("EEEEdMMMM", stufen.first())
        assertTrue("die Leiter wird nicht kuerzer: $stufen", stufen.last().length < stufen.first().length)
    }

    @Test
    fun `ohne Wochentag geht es genauso vom langen zum kurzen Monat`() {
        assertEquals(
            listOf("dMMMM", "dMMM"),
            ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE, onTile = true),
        )
    }

    /** Die Kopfzeile ist schmal und faengt gleich beim kurzen an - eine Stufe, keine Leiter. */
    @Test
    fun `die Kopfzeile bleibt bei der kurzen Form`() {
        assertEquals(listOf("EEEdMMM"), ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE_WEEKDAY, onTile = false))
        assertEquals(listOf("dMMM"), ClockFormat.dateSkeletons(ClockDisplay.TIME_DATE, onTile = false))
    }

    @Test
    fun `ohne Datum gibt es keine Stufen`() {
        assertTrue(ClockFormat.dateSkeletons(ClockDisplay.OFF, onTile = true).isEmpty())
        assertTrue(ClockFormat.dateSkeletons(ClockDisplay.TIME, onTile = true).isEmpty())
    }

    /** Die alte Frage nach *einer* Stufe bleibt beantwortet - sie ist jetzt die erste. */
    @Test
    fun `dateSkeleton ist die erste Stufe`() {
        ClockDisplay.entries.forEach { anzeige ->
            listOf(true, false).forEach { aufKachel ->
                assertEquals(
                    ClockFormat.dateSkeletons(anzeige, aufKachel).firstOrNull(),
                    ClockFormat.dateSkeleton(anzeige, aufKachel),
                )
            }
        }
    }

    /**
     * Gemessen wird mit dem laengsten Datum des Jahres, nicht mit dem heutigen. Sonst
     * haette die Kachel je nach Wochentag ein anderes Aussehen.
     */
    @Test
    fun `das laengste Datum nimmt den laengsten Wochentag und Monat`() {
        val format = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
        val laengstes = ClockFormat.longestDate(format)
        assertTrue("Wednesday fehlt: $laengstes", "Wednesday" in laengstes)
        assertTrue("September fehlt: $laengstes", "September" in laengstes)
    }

    @Test
    fun `die kurze Form ist wirklich kuerzer als die lange`() {
        val lang = ClockFormat.longestDate(SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH))
        val kurz = ClockFormat.longestDate(SimpleDateFormat("EEE, d MMM", Locale.ENGLISH))
        assertTrue("$kurz ist nicht kuerzer als $lang", kurz.length < lang.length)
    }
}
