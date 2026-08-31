package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * Screens anlegen, umbenennen und loeschen.
 *
 * Zwei Dinge sind hier heikel und deshalb hier und nicht in der Oberflaeche geloest:
 * ein neuer Screen ohne Rueckweg waere eine Sackgasse, weil ein Launcher die
 * Zurueck-Geste nicht abfangen darf; und ein geloeschter Screen laesst Kacheln
 * zurueck, die ins Leere zeigen.
 */
object ScreenEdits {

    /**
     * Neuer Screen im selben Raster wie der Ausgangsscreen, mit einer Heim-Kachel
     * auf dem letzten Platz - sonst kommt der Nutzer dort nie wieder weg.
     */
    fun newScreen(id: String, name: String, like: Screen): Screen {
        val lastX = like.cols - 1
        val lastY = like.rows - 1
        return Screen(
            id = id,
            name = name,
            cols = like.cols,
            rows = like.rows,
            cells = listOf(
                Cell(
                    x = lastX,
                    y = lastY,
                    button = Button(action = ButtonAction.Action(Builtin.HOME_SCREEN)),
                ),
            ),
        )
    }

    /** Eine Kennung, die auf keinen bestehenden Screen faellt. */
    fun freeId(config: LauncherConfig, base: String = "screen"): String {
        var index = config.screens.size + 1
        while (config.screens.any { it.id == "$base$index" }) index++
        return "$base$index"
    }

    fun add(config: LauncherConfig, screen: Screen): LauncherConfig =
        if (config.screens.any { it.id == screen.id }) config
        else config.copy(screens = config.screens + screen)

    fun rename(config: LauncherConfig, id: String, name: String): LauncherConfig {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return config
        return config.copy(
            screens = config.screens.map { if (it.id == id) it.copy(name = trimmed) else it },
        )
    }

    /**
     * Loescht einen Screen und raeumt hinter ihm auf: Kacheln, die dorthin sprangen,
     * werden entfernt, und die Wischreihenfolge verliert den Eintrag. Der Startscreen
     * laesst sich nicht loeschen - sonst haette der Launcher kein Zuhause mehr.
     */
    fun delete(config: LauncherConfig, id: String): LauncherConfig {
        if (id == config.homeScreenId) return config
        if (config.screens.none { it.id == id }) return config
        if (config.screens.size <= 1) return config

        val remaining = config.screens
            .filter { it.id != id }
            .map { screen ->
                screen.copy(cells = screen.cells.filterNot { it.button.action == ButtonAction.GoToScreen(id) })
            }
        return config.copy(
            screens = remaining,
            swipeOrder = config.swipeOrder.filterNot { it == id },
        )
    }

    /** Zeigt irgendeine Kachel auf einen Screen, den es nicht gibt? */
    fun danglingReferences(config: LauncherConfig): List<String> {
        val known = config.screens.map { it.id }.toSet()
        return config.screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.GoToScreen)?.screenId }
            .filterNot { it in known }
            .distinct()
    }
}
