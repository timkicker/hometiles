package org.biglau.phone

import org.biglau.data.PhoneConfig
import org.biglau.data.SpeedDialTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialTest {

    private val oma = SpeedDialTarget("Oma", "+436601234567")
    private val opa = SpeedDialTarget("Opa", "+436609876543")

    @Test
    fun `belegbar sind die Tasten zwei bis neun`() {
        ('2'..'9').forEach { assertTrue("$it sollte belegbar sein", SpeedDial.isAssignable(it)) }
    }

    @Test
    fun `null und eins bleiben frei`() {
        // Die 1 ist auf vielen Netzen die Mailbox, die 0 die Auslandsvorwahl - beide zu
        // belegen wuerde Gewohnheiten brechen, die aelter sind als dieses Telefon.
        assertTrue(!SpeedDial.isAssignable('0'))
        assertTrue(!SpeedDial.isAssignable('1'))
        assertTrue(!SpeedDial.isAssignable('#'))
    }

    @Test
    fun `eine belegte Taste liefert ihr Ziel`() {
        val config = SpeedDial.assign(PhoneConfig(), '2', oma)
        assertEquals(oma, SpeedDial.targetFor(config, '2'))
    }

    @Test
    fun `eine unbelegte Taste liefert nichts`() {
        assertNull(SpeedDial.targetFor(PhoneConfig(), '5'))
    }

    @Test
    fun `unbelegbare Tasten lassen sich nicht belegen`() {
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '1', oma))
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '0', oma))
    }

    @Test
    fun `ein Ziel ohne waehlbare Nummer wird abgelehnt`() {
        // Sonst haette der Nutzer eine Taste, die beim Druecken nichts tut.
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '3', SpeedDialTarget("X", "")))
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '3', SpeedDialTarget("X", "keine")))
    }

    @Test
    fun `Neubelegung ersetzt die alte`() {
        var config = SpeedDial.assign(PhoneConfig(), '4', oma)
        config = SpeedDial.assign(config, '4', opa)
        assertEquals(opa, SpeedDial.targetFor(config, '4'))
        assertEquals(1, config.speedDial.size)
    }

    @Test
    fun `dieselbe Person darf auf zwei Tasten liegen`() {
        var config = SpeedDial.assign(PhoneConfig(), '2', oma)
        config = SpeedDial.assign(config, '3', oma)
        assertEquals(oma, SpeedDial.targetFor(config, '2'))
        assertEquals(oma, SpeedDial.targetFor(config, '3'))
    }

    @Test
    fun `Loeschen entfernt nur diese Taste`() {
        var config = SpeedDial.assign(PhoneConfig(), '2', oma)
        config = SpeedDial.assign(config, '3', opa)
        config = SpeedDial.clear(config, '2')
        assertNull(SpeedDial.targetFor(config, '2'))
        assertEquals(opa, SpeedDial.targetFor(config, '3'))
    }

    @Test
    fun `Loeschen einer freien Taste aendert nichts`() {
        val config = SpeedDial.assign(PhoneConfig(), '2', oma)
        assertEquals(config, SpeedDial.clear(config, '7'))
    }

    @Test
    fun `belegte Tasten stehen unter ihrer eigenen Ziffer`() {
        // Die Wähltastatur fragt je Taste nach - eine Gesamtliste braucht sie nicht.
        var config = SpeedDial.assign(PhoneConfig(), '7', opa)
        config = SpeedDial.assign(config, '3', oma)
        assertEquals(oma.name, SpeedDial.targetFor(config, '3')?.name)
        assertEquals(opa.name, SpeedDial.targetFor(config, '7')?.name)
        assertEquals(null, SpeedDial.targetFor(config, '5'))
    }
}
