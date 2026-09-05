package org.biglau.apps

import org.biglau.data.AppsConfig
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Security
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the switch of the app lock.
 *
 * one tap on the emulator tipped it into a state where not a single app opened without the
 * pin: the home screen carried only builtin tiles, a widget and a shortcut - nothing
 * [AppLock.initialAllowance] could have allowed.
 */
class AppLockToggleTest {

    private fun config(
        locked: Boolean = false,
        allowed: Set<String> = emptySet(),
        tileApps: List<String> = emptyList(),
    ): LauncherConfig {
        val base = LauncherConfig()
        val screen = base.screens.first()
        val cells = tileApps.mapIndexed { index, packageName ->
            Cell(
                x = index,
                y = 0,
                button = Button(action = ButtonAction.App(packageName, "$packageName.Main")),
            )
        }
        return base.copy(
            screens = listOf(screen.copy(cells = cells)),
            apps = AppsConfig(lockOthers = locked, allowed = allowed),
            security = Security(pin = "any"),
        )
    }

    @Test
    fun `without tile apps and without an allow list it does not switch on`() {
        assertEquals(AppLock.Step.CHOOSE_FIRST, AppLock.toggle(config()))
    }

    @Test
    fun `with one app on a tile the switch goes on`() {
        assertEquals(
            AppLock.Step.TURN_ON,
            AppLock.toggle(config(tileApps = listOf("com.example.maps"))),
        )
    }

    @Test
    fun `a prepared allow list is enough without a tile app`() {
        // whoever builds the list first and switches on afterwards should not depend on the
        // tiles.
        assertEquals(
            AppLock.Step.TURN_ON,
            AppLock.toggle(config(allowed = setOf("com.example.maps/Main"))),
        )
    }

    @Test
    fun `switching off always works`() {
        // out of the dangerous state too: otherwise there would be no leaving it.
        assertEquals(AppLock.Step.TURN_OFF, AppLock.toggle(config(locked = true)))
    }

    @Test
    fun `the row under the switch counts what would stay open`() {
        assertEquals(0, AppLock.wouldAllow(config()))
        assertEquals(1, AppLock.wouldAllow(config(tileApps = listOf("com.example.maps"))))
        // a hand-built list beats the tiles - it is the later decision.
        assertEquals(
            2,
            AppLock.wouldAllow(
                config(
                    allowed = setOf("a/Main", "b/Main"),
                    tileApps = listOf("com.example.maps"),
                ),
            ),
        )
    }

    @Test
    fun `one allowed app is enough for the switch to go on`() {
        val config = config(allowed = setOf("com.android.camera2/Main"))
        assertEquals(1, AppLock.wouldAllow(config))
        assertEquals(AppLock.Step.TURN_ON, AppLock.toggle(config))
    }

    @Test
    fun `the tile apps land as keys in the allowance`() {
        assertEquals(
            setOf("com.example.maps/com.example.maps.Main"),
            AppLock.initialAllowance(config(tileApps = listOf("com.example.maps"))),
        )
    }

    @Test
    fun `a home screen without app tiles allows nothing`() {
        assertEquals(emptySet<String>(), AppLock.initialAllowance(config()))
    }
}
