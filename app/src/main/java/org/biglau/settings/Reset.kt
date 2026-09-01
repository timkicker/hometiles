package org.biglau.settings

import org.biglau.data.ButtonAction
import org.biglau.data.LauncherConfig
import org.biglau.data.ScreenKind

/**
 * „Alles zurücksetzen" aus PLAN.md 4.9.
 *
 * Der einzige Schritt in dieser App, der nicht rückgängig zu machen ist. Deshalb sagt die
 * Rückfrage nicht „bist du sicher", sondern zählt auf, was verschwindet - drei
 * Bildschirme, vierzehn Kacheln, zwei Ordner. Eine Zahl macht eine Warnung wahr; „bist du
 * sicher" tippt man weg, ohne sie zu lesen.
 */
object Reset {

    /** Was bei einem Zurücksetzen verlorengeht. */
    data class Losses(
        val screens: Int,
        val tiles: Int,
        val folders: Int,
        val hasPin: Boolean,
    )

    fun losses(config: LauncherConfig): Losses {
        val ordner = config.screens.count { it.kind == ScreenKind.FOLDER }
        return Losses(
            screens = config.screens.size - ordner,
            tiles = config.screens.sumOf { screen ->
                screen.cells.count { it.button.action != ButtonAction.None }
            },
            folders = ordner,
            hasPin = config.security.pin != null,
        )
    }

    /**
     * Die Kennungen aller eingebauten Widgets. Ein Zurücksetzen wirft die Kacheln weg;
     * ohne diesen Schritt behielte der Widget-Host sie für immer, und die Anbieter-App
     * hielte ein Widget am Leben, das niemand mehr sieht.
     */
    fun widgetIds(config: LauncherConfig): List<Int> = config.screens
        .flatMap { it.cells }
        .mapNotNull { (it.button.action as? ButtonAction.Widget)?.widgetId }

    /**
     * Der Zustand wie nach der Installation - einschliesslich `wizardDone = false`, damit
     * der Assistent wieder läuft. Ohne ihn stünde man vor einem fremden Startbildschirm
     * ohne Hinweis, was als Nächstes zu tun ist.
     */
    fun fresh(): LauncherConfig = LauncherConfig()

    fun isFresh(config: LauncherConfig): Boolean = config == fresh()
}
