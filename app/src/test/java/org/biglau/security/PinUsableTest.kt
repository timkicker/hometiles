package org.biglau.security

import org.biglau.apps.AppLock
import org.biglau.data.AppsConfig
import org.biglau.data.LauncherConfig
import org.biglau.data.Security
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine PIN, die sich nicht pruefen laesst, ist kein Schloss.
 *
 * Anlass: eine von Hand geschriebene Konfiguration mit `"pin": "1231"` - also der PIN im
 * Klartext statt als „Runden:Salz:Hash". Danach wies die App-Sperre am Emulator jede
 * Eingabe ab, auch die richtige, und liess sich nicht mehr oeffnen: [Pin.verify] steigt bei
 * einem Wert aus, der nicht drei Teile hat. Auf dem Startbildschirm gibt es dagegen keinen
 * Notausstieg.
 */
class PinUsableTest {

    private val echt = Pin.hash("1231")!!

    @Test
    fun `ein gehashter Wert ist pruefbar`() {
        assertTrue(Pin.usable(echt))
        assertTrue(Pin.verify("1231", echt))
    }

    @Test
    fun `Klartext und Unsinn sind nicht pruefbar`() {
        assertFalse(Pin.usable(null))
        assertFalse(Pin.usable("1231"))
        assertFalse(Pin.usable(""))
        assertFalse(Pin.usable("a:b"))
        assertFalse(Pin.usable("keine Zahl:AAAA:AAAA"))
        assertFalse(Pin.usable("20000:kein Base64 !:AAAA"))
    }

    @Test
    fun `ein unpruefbarer Wert schuetzt nichts`() {
        assertTrue(Pin.protects(echt, enabled = true))
        assertFalse(Pin.protects("1231", enabled = true))
        assertFalse(Pin.protects(echt, enabled = false))
    }

    @Test
    fun `die App-Sperre haelt mit einem unpruefbaren Wert niemanden fest`() {
        fun config(pin: String?) = LauncherConfig(
            security = Security(pin = pin),
            apps = AppsConfig(lockOthers = true, allowed = emptySet()),
        )
        assertTrue(AppLock.needsPin(config(echt), "a/b", "a"))
        assertFalse(AppLock.needsPin(config("1231"), "a/b", "a"))
    }
}
