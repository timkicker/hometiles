package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind

/**
 * Ordner anlegen, füllen und wieder loswerden.
 *
 * Ein Ordner ist ein [Screen] mit [ScreenKind.FOLDER]; die Kachel, die ihn öffnet, trägt
 * [ButtonAction.Folder]. Beides gehört zusammen und darf nie auseinanderlaufen - ein Ordner
 * ohne Kachel wäre genau der unerreichbare Screen, vor dem die Einstellungen warnen, und
 * eine Kachel ohne Ordner zeigte ins Leere.
 */
object FolderEdits {

    /** Wie viele Symbole die Kachel eines Ordners zeigt, bevor sie unleserlich wird. */
    const val PREVIEW_COUNT = 4

    /**
     * Ein neuer, leerer Ordner im selben Raster wie der Screen, auf dem er liegt.
     *
     * Bewusst **ohne** Heim-Kachel, anders als ein neuer Screen: ein Ordner schließt sich
     * mit der Zurück-Geste, er kann keine Sackgasse werden.
     */
    fun newFolder(id: String, name: String, like: Screen): Screen = Screen(
        id = id,
        name = name,
        cols = like.cols,
        rows = like.rows,
        cells = emptyList(),
        kind = ScreenKind.FOLDER,
    )

    /** Die Screens, die keine Ordner sind - alles, was die Screen-Verwaltung zeigen darf. */
    fun plainScreens(config: LauncherConfig): List<Screen> = config.screens.filterNot { it.isFolder }

    fun folders(config: LauncherConfig): List<Screen> = config.screens.filter { it.isFolder }

    /** Der Ordner hinter einer Kachel, sofern es ihn gibt. */
    fun folderFor(config: LauncherConfig, action: ButtonAction.Folder): Screen? =
        config.screens.firstOrNull { it.id == action.screenId && it.isFolder }

    /** Die ersten paar Kacheln eines Ordners - das, was auf seiner eigenen Kachel erscheint. */
    fun preview(folder: Screen): List<Cell> =
        folder.cells
            .sortedWith(compareBy({ it.y }, { it.x }))
            .take(PREVIEW_COUNT)

    /**
     * Darf auf diesem Screen eine Ordnerkachel angelegt werden?
     *
     * In einem Ordner nicht. Wer sich durch zwei Ebenen tippt, hat den Überblick verloren,
     * den große Kacheln herstellen sollen.
     */
    fun mayContainFolder(screen: Screen): Boolean = !screen.isFolder

    /**
     * Löscht einen Ordner samt Inhalt und räumt jede Kachel weg, die auf ihn zeigte.
     *
     * Beides zusammen, weil das eine ohne das andere kaputt ist.
     */
    fun delete(config: LauncherConfig, folderId: String): LauncherConfig {
        val folder = config.screens.firstOrNull { it.id == folderId && it.isFolder } ?: return config
        return config.copy(
            screens = config.screens
                .filterNot { it.id == folder.id }
                .map { screen ->
                    screen.copy(
                        cells = screen.cells.filterNot {
                            (it.button.action as? ButtonAction.Folder)?.screenId == folder.id
                        },
                    )
                },
        )
    }

    /** Wie viele Kacheln ein Löschen kostet - steht in der Rückfrage. */
    fun contentCount(config: LauncherConfig, folderId: String): Int =
        config.screens.firstOrNull { it.id == folderId && it.isFolder }?.cells?.size ?: 0

    /** Ordner, auf die keine Kachel mehr zeigt. Die sind für niemanden mehr erreichbar. */
    fun orphaned(config: LauncherConfig): List<Screen> {
        val verwendet = config.screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .toSet()
        return folders(config).filterNot { it.id in verwendet }
    }
}
