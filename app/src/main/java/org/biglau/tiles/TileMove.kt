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
     * Ein Platz auf demselben Bildschirm, auf den die Kachel darf.
     *
     * [occupant] ist die Kachel, die dort schon liegt - dann wird getauscht, nicht
     * ueberschrieben. Eine Kachel, die beim Verschieben eine andere verschluckt, waere
     * genau der Verlust, den ein Verschieben verhindern soll.
     */
    data class Spot(val x: Int, val y: Int, val occupant: Cell?)

    /**
     * Wohin die Kachel auf ihrem eigenen Bildschirm darf.
     *
     * `PLAN.md` 4.1 sagt "Kacheln tauschen" zu und begruendet, warum es kein Ziehen gibt:
     * Ziehen setzt eine ruhige Hand voraus. Der Ersatz war halb gebaut - verschieben ging
     * nur auf einen *anderen* Screen. Wer die Reihenfolge auf seinem Startbildschirm
     * aendern wollte, musste die Kachel loeschen und neu anlegen.
     *
     * Angeboten wird nur, was ohne Verlust ausgeht: ein freier Platz, auf den die Kachel in
     * ihrer Groesse passt, und eine gleich grosse Nachbarkachel zum Tauschen. Bei
     * ungleichen Groessen bliebe beim Tausch ein Loch oder eine Ueberdeckung.
     */
    fun spotsFor(screen: Screen, cell: Cell): List<Spot> =
        (0 until screen.rows).flatMap { y -> (0 until screen.cols).map { x -> x to y } }
            .filterNot { (x, y) -> cell.covers(x, y) }
            .mapNotNull { (x, y) ->
                when (val belegt = screen.cellAt(x, y)) {
                    null -> if (passt(screen, cell, x, y)) Spot(x, y, null) else null
                    // Nur an der Ecke der Nachbarkachel, sonst stuende dieselbe Kachel bei
                    // einer 2x2 viermal in der Liste.
                    else -> if (belegt.x == x && belegt.y == y && belegt.w == cell.w && belegt.h == cell.h) {
                        Spot(x, y, belegt)
                    } else {
                        null
                    }
                }
            }

    /** Passt die Kachel in ihrer Groesse auf diesen Platz, ohne ueber den Rand oder auf eine andere? */
    private fun passt(screen: Screen, cell: Cell, x: Int, y: Int): Boolean {
        if (x + cell.w > screen.cols || y + cell.h > screen.rows) return false
        return (y until y + cell.h).all { py ->
            (x until x + cell.w).all { px ->
                val da = screen.cellAt(px, py)
                da == null || (da.x == cell.x && da.y == cell.y)
            }
        }
    }

    /**
     * Verschiebt die Kachel innerhalb ihres Bildschirms - auf einen freien Platz oder im
     * Tausch mit der Kachel, die dort liegt.
     *
     * Gibt `null` zurueck, wenn der Platz nicht in [spotsFor] steht. Zwischen dem Anzeigen
     * der Liste und dem Antippen kann sich die Konfiguration geaendert haben.
     */
    fun moveWithin(config: LauncherConfig, screenId: String, x: Int, y: Int, toX: Int, toY: Int): LauncherConfig? {
        val screen = config.screens.firstOrNull { it.id == screenId } ?: return null
        val kachel = screen.cellAt(x, y) ?: return null
        val platz = spotsFor(screen, kachel).firstOrNull { it.x == toX && it.y == toY } ?: return null
        val getauscht = screen.cells.map { zelle ->
            when {
                zelle.x == kachel.x && zelle.y == kachel.y -> zelle.copy(x = platz.x, y = platz.y)
                platz.occupant != null && zelle.x == platz.occupant.x && zelle.y == platz.occupant.y ->
                    zelle.copy(x = kachel.x, y = kachel.y)
                else -> zelle
            }
        }
        return config.copy(
            screens = config.screens.map { if (it.id == screenId) it.copy(cells = getauscht) else it },
        )
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
