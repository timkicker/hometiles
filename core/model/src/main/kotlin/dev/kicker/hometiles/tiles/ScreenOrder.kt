package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen

/**
 * the screen order for "next" and "previous".
 *
 * folders do not take part: they belong to their tile, not to the row. the row is a ring,
 * because a row with ends would give two tiles that sometimes do nothing.
 */
object ScreenOrder {

    fun ordered(config: LauncherConfig): List<Screen> {
        val real = config.screens.filterNot { it.isFolder || it.id in config.swipeExcluded }
        if (config.swipeOrder.isEmpty()) return real
        // the explicitly ordered ones first, then the rest in their natural sequence.
        val byOrder = config.swipeOrder.mapNotNull { id -> real.firstOrNull { it.id == id } }
        return byOrder + real.filterNot { it.id in config.swipeOrder }
    }

    fun next(config: LauncherConfig, currentId: String): String? = step(config, currentId, +1)

    fun previous(config: LauncherConfig, currentId: String): String? = step(config, currentId, -1)

    private fun step(config: LauncherConfig, currentId: String, direction: Int): String? {
        val row = ordered(config)
        if (row.size < 2) return null
        val here = row.indexOfFirst { it.id == currentId }
        if (here < 0) return null
        val next = ((here + direction) % row.size + row.size) % row.size
        return row[next].id
    }
}
