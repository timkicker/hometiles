package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * Einen Screen verdoppeln. PLAN.md 4.1.
 *
 * Gedacht zum Ausprobieren: eine neue Anordnung bauen, ohne die alte zu verlieren.
 * Deshalb ist der wichtigste Teil nicht das Kopieren, sondern was **nicht** mitkopiert
 * wird - und dass man es erfaehrt.
 */
object ScreenCopy {

    sealed interface Result {
        /**
         * @param skippedWidgets Widgets, die leer geblieben sind
         * @param skippedFolders Ordnerkacheln, die leer geblieben sind
         */
        data class Done(
            val config: LauncherConfig,
            val newId: String,
            val copied: Int,
            val skippedWidgets: Int,
            val skippedFolders: Int,
        ) : Result

        /** Auf dem Original ist keine Zelle frei fuer die Kachel, die zur Kopie fuehrt. */
        data object NoRoomForJumpTile : Result

        data object NoSuchScreen : Result
    }

    /**
     * Widgets werden nicht mitkopiert. Eine Widget-Kachel haelt eine Kennung, die der
     * AppWidgetHost genau einmal vergeben hat; zweimal dieselbe hiesse, dass das Loeschen
     * der einen Kachel die andere kaputtmacht. Ein Widget legt man in der Kopie neu an.
     *
     * Ordnerkacheln auch nicht. Zu einem Ordner gehoert genau eine Kachel - sein Loeschen
     * raeumt beides zusammen weg. Zwei Kacheln auf denselben Ordner liessen nach dem
     * Loeschen eine ins Leere zeigen.
     */
    private fun copyable(action: ButtonAction): Boolean =
        action !is ButtonAction.Widget && action !is ButtonAction.Folder

    fun duplicate(config: LauncherConfig, id: String, name: String): Result {
        val original = config.screens.firstOrNull { it.id == id } ?: return Result.NoSuchScreen
        // Erst der Weg hin, dann die Kopie: ein Screen, zu dem keine Kachel fuehrt, ist
        // eingerichtet und unauffindbar. Lieber gar nicht verdoppeln als so.
        val platz = original.freeSlots().firstOrNull() ?: return Result.NoRoomForJumpTile

        val neueId = ScreenEdits.freeId(config)
        val behalten = original.cells.filter { copyable(it.button.action) }
        val kopie = Screen(
            id = neueId,
            name = name,
            cols = original.cols,
            rows = original.rows,
            cells = behalten,
        )
        val mitKopie = config.copy(
            screens = config.screens.map { screen ->
                if (screen.id != id) {
                    screen
                } else {
                    screen.copy(
                        cells = screen.cells + Cell(
                            x = platz.first,
                            y = platz.second,
                            button = Button(action = ButtonAction.GoToScreen(neueId)),
                        ),
                    )
                }
            } + kopie,
        )
        return Result.Done(
            config = mitKopie,
            newId = neueId,
            copied = behalten.count { it.button.action != ButtonAction.None },
            skippedWidgets = original.cells.count { it.button.action is ButtonAction.Widget },
            skippedFolders = original.cells.count { it.button.action is ButtonAction.Folder },
        )
    }
}
