package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Uhrzeit, ein Muster.
 *
 * Am 3.9.2026 gefunden: `"HH:mm"` stand in drei Dateien. Kopfzeile und Infokachel fragten
 * vorher, ob das Telefon auf 12 oder 24 Stunden steht — die **Nachrichtenliste** nicht. Auf
 * diesem Gerät (12 Stunden) stand oben „2:30 PM" und in der Liste derselben Minute „14:30".
 *
 * Für jemanden, der schlecht liest, sind das zwei verschiedene Uhrzeiten.
 */
class TimePatternTest {

    @Test
    fun `vierundzwanzig Stunden ohne AM und PM`() {
        assertEquals("HH:mm", ClockFormat.timePattern(twentyFourHour = true))
    }

    @Test
    fun `zwoelf Stunden mit AM und PM`() {
        val muster = ClockFormat.timePattern(twentyFourHour = false)
        assertTrue("ohne 'a' fehlt AM/PM und 13 Uhr sähe aus wie 1 Uhr", muster.contains("a"))
        assertTrue("die Stunde darf nicht zweistellig erzwungen sein", muster.startsWith("h:"))
    }
}
