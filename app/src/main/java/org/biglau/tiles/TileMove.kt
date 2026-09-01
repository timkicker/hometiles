package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * Eine Kachel von einem Screen oder Ordner in einen anderen bringen.
 *
 * Ohne das ist ein Ordner eine Einbahnstraße: man legt etwas hinein und bekommt es nur
 * heraus, indem man es dort löscht und woanders neu anlegt - bei einem Kontakt mit der
 * richtigen Nummer und der richtigen Betriebsart ist das ärgerlich genug, um es zu lassen.
 */
object TileMove {

    /**
     * Wohin eine Kachel überhaupt darf.
     *
     * Ein Ordner darf nicht in einen Ordner: zwei Ebenen zerstören den Überblick, den große
     * Kacheln herstellen sollen (`PLAN.md` 4.9). Und nirgendwohin, wo kein Platz ist - eine
     * Kachel, die beim Verschieben still verschwindet, wäre der schlimmste Ausgang.
     */
    fun targetsFor(config: LauncherConfig, fromScreenId: String, cell: Cell): List<Screen> {
        val istOrdner = cell.button.action is ButtonAction.Folder
        return config.screens
            .filterNot { it.id == fromScreenId }
            .filterNot { istOrdner && it.isFolder }
            .filter { it.freeSlots().isNotEmpty() }
    }

    /**
     * Verschiebt die Kachel auf den ersten freien Platz des Ziels, von links oben gelesen.
     *
     * Gibt `null` zurück, wenn es nicht geht - der Aufrufer soll dann nichts tun und es
     * sagen, statt eine halbe Verschiebung stehen zu lassen.
     */
    fun move(config: LauncherConfig, fromScreenId: String, x: Int, y: Int, toScreenId: String): LauncherConfig? {
        if (fromScreenId == toScreenId) return null
        val quelle = config.screens.firstOrNull { it.id == fromScreenId } ?: return null
        val ziel = config.screens.firstOrNull { it.id == toScreenId } ?: return null
        val kachel = quelle.cellAt(x, y) ?: return null
        if (kachel.button.action is ButtonAction.Folder && ziel.isFolder) return null

        val platz = ziel.freeSlots().firstOrNull() ?: return null

        return config.copy(
            screens = config.screens.map { screen ->
                when (screen.id) {
                    quelle.id -> screen.copy(cells = screen.cells.filterNot { it.x == kachel.x && it.y == kachel.y })
                    ziel.id -> screen.copy(
                        cells = screen.cells + kachel.copy(
                            x = platz.first,
                            y = platz.second,
                            // Eine breite Kachel passt nicht unbesehen woandershin; sie
                            // kommt einfeldrig an, statt ueber den Rand zu ragen.
                            w = 1,
                            h = 1,
                        ),
                    )
                    else -> screen
                }
            },
        )
    }
}
