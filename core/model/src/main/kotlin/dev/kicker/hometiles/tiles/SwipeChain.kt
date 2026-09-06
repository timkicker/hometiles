package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen

/**
 * the screen order for swiping and for the "next" and "previous" tiles. `PLAN.md` 4.1.
 *
 * moved one place up or down, not dragged: dragging assumes a steady hand, and that is not
 * to be assumed for the people this app is for.
 */
object SwipeChain {

    fun explicit(config: LauncherConfig): List<String> =
        ScreenOrder.ordered(config).map { it.id }

    fun moveUp(config: LauncherConfig, id: String): LauncherConfig = move(config, id, -1)

    fun moveDown(config: LauncherConfig, id: String): LauncherConfig = move(config, id, +1)

    /** zero-based, or -1 when the screen is not in the chain. */
    fun position(config: LauncherConfig, id: String): Int = explicit(config).indexOf(id)

    private fun move(config: LauncherConfig, id: String, direction: Int): LauncherConfig {
        val row = explicit(config).toMutableList()
        val here = row.indexOf(id)
        val target = here + direction
        // nothing at the edges, and no wraparound: whoever taps "up" on the topmost entry
        // does not expect it to come out at the bottom.
        if (here < 0 || target !in row.indices) return config
        row[here] = row[target]
        row[target] = id
        return config.copy(swipeOrder = row)
    }

    /**
     * may this screen leave the chain?
     *
     * only if it stays reachable afterwards, by a jump tile or by being the home screen.
     * otherwise removing it would be the quickest way to make a set-up screen unfindable.
     */
    fun mayLeave(config: LauncherConfig, id: String): Boolean =
        id == config.homeScreenId || hasJumpTile(config, id)

    fun exclude(config: LauncherConfig, id: String): LauncherConfig =
        if (!mayLeave(config, id)) config else config.copy(swipeExcluded = config.swipeExcluded + id)

    fun include(config: LauncherConfig, id: String): LauncherConfig =
        config.copy(swipeExcluded = config.swipeExcluded - id)

    fun excluded(config: LauncherConfig): List<Screen> =
        config.screens.filter { !it.isFolder && it.id in config.swipeExcluded }

    private fun hasJumpTile(config: LauncherConfig, id: String): Boolean = config.screens
        .flatMap { it.cells }
        .any { (it.button.action as? ButtonAction.GoToScreen)?.screenId == id }
}
