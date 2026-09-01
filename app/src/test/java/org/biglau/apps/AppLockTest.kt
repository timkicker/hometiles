package org.biglau.apps

import org.biglau.data.AppsConfig
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.Security
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.5: „App-Sperre: welche Apps ohne PIN startbar sind (Whitelist)".
 *
 * Gedacht für den Fall, in dem jemand ein Telefon für eine andere Person einrichtet und
 * will, dass nur ein paar Apps offenstehen. Eine Erlaubnisliste und keine Sperrliste: eine
 * Sperrliste müsste jede App des Telefons nennen und wäre nach der nächsten Installation
 * schon unvollständig.
 */
class AppLockTest {

    private val config = LauncherConfig(
        screens = listOf(
            Screen(
                "home", "Start", 2, 3,
                cells = listOf(
                    Cell(0, 0, button = Button(action = ButtonAction.App("com.wa", "Main"))),
                    Cell(1, 0, button = Button(action = ButtonAction.App("com.maps", "Main"))),
                ),
            ),
        ),
        security = Security(pin = "hash"),
        apps = AppsConfig(lockOthers = true, allowed = setOf("com.wa/Main")),
    )

    @Test
    fun `eine erlaubte app startet ohne pin`() {
        assertEquals(false, AppLock.needsPin(config, "com.wa/Main", "com.wa"))
    }

    @Test
    fun `eine andere app fragt nach der pin`() {
        assertEquals(true, AppLock.needsPin(config, "com.spiel/Main", "com.spiel"))
    }

    /**
     * Ohne gesetzte PIN sperrt nichts. Sonst stünde man vor einem Telefon, auf dem nichts
     * aufgeht und nichts nach etwas fragt, das man eingeben könnte.
     */
    @Test
    fun `ohne pin sperrt nichts`() {
        val ohne = config.copy(security = Security(pin = null))
        assertEquals(false, AppLock.needsPin(ohne, "com.spiel/Main", "com.spiel"))
    }

    @Test
    fun `ausgeschaltet sperrt nichts`() {
        val aus = config.copy(apps = config.apps.copy(lockOthers = false))
        assertEquals(false, AppLock.needsPin(aus, "com.spiel/Main", "com.spiel"))
    }

    // Ein Paketname erlaubt alle seine Einstiege - dieselbe Regel wie beim Ausblenden.
    @Test
    fun `ein paketname erlaubt die ganze app`() {
        val perPaket = config.copy(apps = config.apps.copy(allowed = setOf("com.spiel")))
        assertEquals(false, AppLock.needsPin(perPaket, "com.spiel/Zweiter", "com.spiel"))
    }

    /**
     * Der wichtigste Teil: beim Einschalten sind die Apps auf den Kacheln von selbst
     * erlaubt. Wer die Sperre einschaltet und danach vor einem Telefon steht, auf dem
     * nichts mehr aufgeht, hat sich ausgesperrt statt etwas gesichert.
     */
    @Test
    fun `die kachel-apps sind von anfang an erlaubt`() {
        assertEquals(
            setOf("com.wa/Main", "com.maps/Main"),
            AppLock.initialAllowance(config),
        )
    }

    @Test
    fun `erlauben und sperren ist derselbe tipp`() {
        val ohne = AppLock.toggleAllowed(config.apps, "com.wa/Main")
        assertEquals(false, ohne.allowed.contains("com.wa/Main"))
        val wieder = AppLock.toggleAllowed(ohne, "com.wa/Main")
        assertEquals(true, wieder.allowed.contains("com.wa/Main"))
    }

    @Test
    fun `die vorgabe sperrt nichts`() {
        assertEquals(false, AppsConfig().lockOthers)
        assertEquals(emptySet<String>(), AppsConfig().allowed)
    }
}

/**
 * Wird die PIN entfernt, geht die App-Sperre mit.
 *
 * Beim ersten Mal blieb sie stehen: die Schutzschalter liegen in `security`, die App-Sperre
 * in `apps`, und das Zurücksetzen traf nur den einen Ort. Ohne PIN sperrt sie zwar nichts —
 * aber ihre Zeile steht nur da, solange eine PIN gesetzt ist, man käme also nicht mehr an
 * sie heran, und die nächste gesetzte PIN sperrte ungefragt jede App zu, die auf keiner
 * Kachel liegt.
 */
class AppLockRemovalTest {

    @Test
    fun `ohne pin steht die sperre wieder auf vorgabe`() {
        val frisch = AppsConfig()
        assertEquals(false, frisch.lockOthers)
        assertEquals(emptySet<String>(), frisch.allowed)
    }

    @Test
    fun `eine stehengebliebene sperre wuerde ohne pin nichts tun`() {
        val stehengeblieben = LauncherConfig(
            security = Security(pin = null),
            apps = AppsConfig(lockOthers = true, allowed = emptySet()),
        )
        assertEquals(false, AppLock.needsPin(stehengeblieben, "com.x/Main", "com.x"))
    }
}
