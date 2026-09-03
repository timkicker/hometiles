package org.biglau.info

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Signalkachel.
 *
 * Vier Zustände, weil sie zu verschiedenen Handlungen führen: keine Karte, kein Netz,
 * schwacher Empfang, Empfang in Ordnung. Ein leerer Balken für alle drei ersten Fälle
 * sagt nicht, was zu tun ist.
 */
class SignalInfoTest {

    private fun lesung(
        level: Int = 3,
        hasSim: Boolean = true,
        inService: Boolean = true,
        roaming: Boolean = false,
        networkType: String = "4G",
    ) = SignalReading(
        level = level,
        mayRead = true,
        hasSim = hasSim,
        inService = inService,
        roaming = roaming,
        networkType = networkType,
    )

    @Test
    fun `ohne Karte ist alles andere gleichgueltig`() {
        val ohne = lesung(level = 4, hasSim = false)
        assertEquals(SignalInfo.State.NO_SIM, SignalInfo.stateOf(ohne))
        assertEquals(0, SignalInfo.bars(ohne))
        assertEquals("", SignalInfo.caption(ohne))
    }

    @Test
    fun `Karte ohne Netz ist etwas anderes als kein Empfang`() {
        val kein = lesung(level = 2, inService = false)
        assertEquals(SignalInfo.State.NO_SERVICE, SignalInfo.stateOf(kein))
        assertEquals(0, SignalInfo.bars(kein))
    }

    @Test
    fun `ein Balken oder keiner heisst schwach`() {
        assertEquals(SignalInfo.State.WEAK, SignalInfo.stateOf(lesung(level = 0)))
        assertEquals(SignalInfo.State.WEAK, SignalInfo.stateOf(lesung(level = 1)))
        assertEquals(SignalInfo.State.OK, SignalInfo.stateOf(lesung(level = 2)))
    }

    @Test
    fun `unbekannt zaehlt als leer und nicht als voll`() {
        // getLevel liefert -1, wenn das Netz nichts meldet. Ein negativer Wert als Balken
        // waere ein Absturz oder ein voller Balken - beides falsch.
        assertEquals(0, SignalInfo.bars(lesung(level = -1)))
    }

    @Test
    fun `mehr als vier Balken gibt es nicht`() {
        assertEquals(SignalInfo.MAX_LEVEL, SignalInfo.bars(lesung(level = 9)))
    }

    @Test
    fun `Roaming steht auf der Kachel`() {
        // Es kostet Geld, und in der Systemleiste eines Drei-Zoll-Geraets uebersieht man es.
        assertEquals("R 4G", SignalInfo.caption(lesung(roaming = true)))
        assertEquals("R", SignalInfo.caption(lesung(roaming = true, networkType = "")))
        assertEquals("4G", SignalInfo.caption(lesung(roaming = false)))
    }

    @Test
    fun `ohne Netz steht kein Zusatz da`() {
        assertEquals("", SignalInfo.caption(lesung(inService = false, roaming = true)))
    }

    @Test
    fun `Leerzeichen in der Netzart stoeren nicht`() {
        assertEquals("LTE", SignalInfo.caption(lesung(networkType = "  LTE  ")))
    }
}

/**
 * „Weiß nichts" ist etwas anderes als „keine Karte".
 *
 * Beim ersten Lauf am Gerät meldete die Kachel „Keine SIM-Karte", während die Karte steckte
 * und die Systemleiste 4G zeigte - BigLau hatte nur die Leseberechtigung nicht. Eine Anzeige,
 * die eine fehlende Karte behauptet, schickt den Nutzer den Deckel aufschrauben.
 */
class SignalPermissionTest {

    @Test
    fun `ohne Leseerlaubnis sagt die Kachel das auch`() {
        val ohne = SignalReading(
            level = -1,
            mayRead = false,
            hasSim = false,
            inService = false,
            roaming = false,
            networkType = "",
        )
        assertEquals(SignalInfo.State.NO_PERMISSION, SignalInfo.stateOf(ohne))
        assertEquals(0, SignalInfo.bars(ohne))
        assertEquals("", SignalInfo.caption(ohne))
    }

    @Test
    fun `die Erlaubnis geht allem anderen vor`() {
        // Selbst wenn irgendwoher Werte kaemen: ohne Erlaubnis sind sie nicht zu trauen.
        val widerspruch = SignalReading(
            level = 4,
            mayRead = false,
            hasSim = true,
            inService = true,
            roaming = false,
            networkType = "4G",
        )
        assertEquals(SignalInfo.State.NO_PERMISSION, SignalInfo.stateOf(widerspruch))
    }

    @Test
    fun `mit Erlaubnis und ohne Karte bleibt es bei keine Karte`() {
        val ohneKarte = SignalReading(
            level = -1,
            mayRead = true,
            hasSim = false,
            inService = false,
            roaming = false,
            networkType = "",
        )
        assertEquals(SignalInfo.State.NO_SIM, SignalInfo.stateOf(ohneKarte))
    }
}
