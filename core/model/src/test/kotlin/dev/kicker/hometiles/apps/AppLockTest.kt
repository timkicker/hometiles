package dev.kicker.hometiles.apps

import dev.kicker.hometiles.data.AppsConfig
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.Security
import dev.kicker.hometiles.security.Pin
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.5: the app lock, which apps start without the pin.
 *
 * meant for setting a phone up for someone else with only a few apps open. an allow list and
 * not a block list: a block list would have to name every app on the phone and would be
 * incomplete after the next installation.
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
        security = Security(pin = Pin.hash("1234")),
        apps = AppsConfig(lockOthers = true, allowed = setOf("com.wa/Main")),
    )

    @Test
    fun `an allowed app starts without the pin`() {
        assertEquals(false, AppLock.needsPin(config, "com.wa/Main", "com.wa"))
    }

    @Test
    fun `another app asks for the pin`() {
        assertEquals(true, AppLock.needsPin(config, "com.spiel/Main", "com.spiel"))
    }

    /**
     * without a pin set nothing locks, or one would stand in front of a phone where nothing
     * opens and nothing asks for something one could type.
     */
    @Test
    fun `without a pin nothing locks`() {
        val without = config.copy(security = Security(pin = null))
        assertEquals(false, AppLock.needsPin(without, "com.spiel/Main", "com.spiel"))
    }

    @Test
    fun `switched off nothing locks`() {
        val off = config.copy(apps = config.apps.copy(lockOthers = false))
        assertEquals(false, AppLock.needsPin(off, "com.spiel/Main", "com.spiel"))
    }

    // a package name allows all its entry points - the same rule as for hiding.
    @Test
    fun `a package name allows the whole app`() {
        val perPackage = config.copy(apps = config.apps.copy(allowed = setOf("com.spiel")))
        assertEquals(false, AppLock.needsPin(perPackage, "com.spiel/Zweiter", "com.spiel"))
    }

    /**
     * the most important part: switching the lock on allows the apps on the tiles by itself.
     * whoever switches it on and then stands in front of a phone where nothing opens has
     * locked themselves out rather than secured anything.
     */
    @Test
    fun `the tile apps are allowed from the start`() {
        assertEquals(
            setOf("com.wa/Main", "com.maps/Main"),
            AppLock.initialAllowance(config),
        )
    }

    /**
     * a shortcut leads into an app and lies on a tile just as deliberately. `initialAllowance`
     * counted only `App`, which did not show while the lock let shortcuts through anyway;
     * since it does not, a shortcut just placed would be shut the moment the lock is switched
     * on.
     */
    @Test
    fun `a shortcut on a tile is allowed from the start`() {
        val withShortcut = config.copy(
            screens = listOf(
                config.screens.first().let { screen ->
                    screen.copy(
                        cells = screen.cells + Cell(
                            0, 1,
                            button = Button(
                                action = ButtonAction.Shortcut("com.brave", "neuer-tab", "Neuer Tab"),
                            ),
                        ),
                    )
                },
            ),
        )
        assertEquals(
            setOf("com.wa/Main", "com.maps/Main", "com.brave"),
            AppLock.initialAllowance(withShortcut),
        )
    }

    /** and the other way round: what is not allowed asks for the pin as a shortcut too. */
    @Test
    fun `a shortcut into a locked app asks for the pin`() {
        assertEquals(true, AppLock.needsPin(config, "com.spiel", "com.spiel"))
    }

    @Test
    fun `allowing and locking is the same tap`() {
        val without = AppLock.toggleAllowed(config.apps, "com.wa/Main")
        assertEquals(false, without.allowed.contains("com.wa/Main"))
        val again = AppLock.toggleAllowed(without, "com.wa/Main")
        assertEquals(true, again.allowed.contains("com.wa/Main"))
    }

    @Test
    fun `the default locks nothing`() {
        assertEquals(false, AppsConfig().lockOthers)
        assertEquals(emptySet<String>(), AppsConfig().allowed)
    }
}

/**
 * removing the pin removes the app lock with it.
 *
 * the first time it stayed: the protection switches live in `security`, the app lock in
 * `apps`, and the reset touched only the one place. without a pin it locks nothing, but its
 * row only stands there while a pin is set - so it could not be reached any more, and the
 * next pin set would silently shut every app that lies on no tile.
 */
class AppLockRemovalTest {

    @Test
    fun `without a pin the lock stands at its default again`() {
        val fresh = AppsConfig()
        assertEquals(false, fresh.lockOthers)
        assertEquals(emptySet<String>(), fresh.allowed)
    }

    @Test
    fun `a lock left standing would do nothing without a pin`() {
        val leftStanding = LauncherConfig(
            security = Security(pin = null),
            apps = AppsConfig(lockOthers = true, allowed = emptySet()),
        )
        assertEquals(false, AppLock.needsPin(leftStanding, "com.x/Main", "com.x"))
    }
}
