package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.PhoneConfig
import dev.kicker.hometiles.data.SpeedDialTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialTest {

    private val alex = SpeedDialTarget("Alex", "+436601234567")
    private val robin = SpeedDialTarget("Robin", "+436609876543")

    @Test
    fun `keys two to nine can be assigned`() {
        ('2'..'9').forEach { assertTrue("$it should be assignable", SpeedDial.isAssignable(it)) }
    }

    @Test
    fun `zero and one stay free`() {
        // on many networks 1 is the mailbox and 0 the international prefix - assigning
        // either would break habits older than this phone.
        assertTrue(!SpeedDial.isAssignable('0'))
        assertTrue(!SpeedDial.isAssignable('1'))
        assertTrue(!SpeedDial.isAssignable('#'))
    }

    @Test
    fun `an assigned key gives its target`() {
        val config = SpeedDial.assign(PhoneConfig(), '2', alex)
        assertEquals(alex, SpeedDial.targetFor(config, '2'))
    }

    @Test
    fun `an unassigned key gives nothing`() {
        assertNull(SpeedDial.targetFor(PhoneConfig(), '5'))
    }

    @Test
    fun `keys that cannot be assigned are not assigned`() {
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '1', alex))
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '0', alex))
    }

    @Test
    fun `a target without a dialable number is refused`() {
        // otherwise there would be a key that does nothing when pressed.
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '3', SpeedDialTarget("X", "")))
        assertEquals(PhoneConfig(), SpeedDial.assign(PhoneConfig(), '3', SpeedDialTarget("X", "none")))
    }

    @Test
    fun `a new assignment replaces the old one`() {
        var config = SpeedDial.assign(PhoneConfig(), '4', alex)
        config = SpeedDial.assign(config, '4', robin)
        assertEquals(robin, SpeedDial.targetFor(config, '4'))
        assertEquals(1, config.speedDial.size)
    }

    @Test
    fun `the same person may sit on two keys`() {
        var config = SpeedDial.assign(PhoneConfig(), '2', alex)
        config = SpeedDial.assign(config, '3', alex)
        assertEquals(alex, SpeedDial.targetFor(config, '2'))
        assertEquals(alex, SpeedDial.targetFor(config, '3'))
    }

    @Test
    fun `clearing removes only that key`() {
        var config = SpeedDial.assign(PhoneConfig(), '2', alex)
        config = SpeedDial.assign(config, '3', robin)
        config = SpeedDial.clear(config, '2')
        assertNull(SpeedDial.targetFor(config, '2'))
        assertEquals(robin, SpeedDial.targetFor(config, '3'))
    }

    @Test
    fun `clearing a free key changes nothing`() {
        val config = SpeedDial.assign(PhoneConfig(), '2', alex)
        assertEquals(config, SpeedDial.clear(config, '7'))
    }

    @Test
    fun `assigned keys stand under their own digit`() {
        // the keypad asks per key - it needs no list of them all.
        var config = SpeedDial.assign(PhoneConfig(), '7', robin)
        config = SpeedDial.assign(config, '3', alex)
        assertEquals(alex.name, SpeedDial.targetFor(config, '3')?.name)
        assertEquals(robin.name, SpeedDial.targetFor(config, '7')?.name)
        assertEquals(null, SpeedDial.targetFor(config, '5'))
    }
}
