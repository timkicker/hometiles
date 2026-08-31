package org.biglau.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {

    @Test
    fun `gueltig sind vier bis acht Ziffern`() {
        assertTrue(Pin.isValid("1234"))
        assertTrue(Pin.isValid("12345678"))
        assertTrue(!Pin.isValid("123"))
        assertTrue(!Pin.isValid("123456789"))
        assertTrue(!Pin.isValid(""))
    }

    @Test
    fun `Buchstaben sind keine PIN`() {
        assertTrue(!Pin.isValid("12a4"))
        assertTrue(!Pin.isValid("abcd"))
        assertTrue(!Pin.isValid("12 34"))
    }

    @Test
    fun `eine ungueltige PIN laesst sich nicht speichern`() {
        assertNull(Pin.hash("123"))
        assertNull(Pin.hash("abcd"))
    }

    @Test
    fun `die richtige PIN wird erkannt`() {
        val stored = Pin.hash("2468")!!
        assertTrue(Pin.verify("2468", stored))
    }

    @Test
    fun `eine falsche PIN wird abgelehnt`() {
        val stored = Pin.hash("2468")!!
        assertTrue(!Pin.verify("2469", stored))
        assertTrue(!Pin.verify("246", stored))
        assertTrue(!Pin.verify("", stored))
    }

    @Test
    fun `ohne gesetzte PIN ist alles offen`() {
        assertTrue(Pin.verify("", null))
        assertTrue(Pin.verify("9999", null))
    }

    @Test
    fun `dieselbe PIN ergibt zweimal verschiedene Werte`() {
        // Sonst liesse sich aus zwei Sicherungsdateien ablesen, dass dieselbe PIN gilt.
        val first = Pin.hash("1234")!!
        val second = Pin.hash("1234")!!
        assertTrue(first != second)
        assertTrue(Pin.verify("1234", first))
        assertTrue(Pin.verify("1234", second))
    }

    @Test
    fun `die PIN steht nicht im Klartext im gespeicherten Wert`() {
        val stored = Pin.hash("13579")!!
        assertTrue(!stored.contains("13579"))
    }

    @Test
    fun `der gespeicherte Wert hat drei durch Doppelpunkt getrennte Teile`() {
        val parts = Pin.hash("1234")!!.split(":")
        assertEquals(3, parts.size)
        assertEquals(20_000, parts[0].toInt())
    }

    @Test
    fun `ein zerstoerter gespeicherter Wert sperrt statt zu oeffnen`() {
        // Im Zweifel zu sperren ist richtig: eine kaputte Datei darf die Einstellungen
        // nicht versehentlich freigeben. Der Notausstieg bleibt der Weg zurueck.
        listOf("", "kaputt", "1:2", "a:b:c", "20000:!!!:???").forEach { broken ->
            assertTrue("'$broken' haette sperren muessen", !Pin.verify("1234", broken))
        }
    }

    @Test
    fun `ein anderer Salzwert ergibt einen anderen Hash`() {
        val a = Pin.hash("1234", ByteArray(16) { 1 })!!
        val b = Pin.hash("1234", ByteArray(16) { 2 })!!
        assertTrue(a != b)
    }

    @Test
    fun `derselbe Salzwert ergibt denselben Hash`() {
        val salt = ByteArray(16) { 7 }
        assertEquals(Pin.hash("1234", salt), Pin.hash("1234", salt))
    }
}
