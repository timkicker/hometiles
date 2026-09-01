package org.biglau.tiles

import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * Die Reihenfolge der Screens für „nächster" und „voriger".
 *
 * Ordner zählen nicht mit: sie gehören ihrer Kachel und liegen nicht in der Reihe. Wer
 * „weiter" tippt, erwartet den nächsten Bildschirm, nicht den Inhalt eines Ordners.
 *
 * Die Reihe ist ein Ring - vom letzten Screen führt „weiter" auf den ersten zurück. Eine
 * Reihe mit Enden hätte zwei Kacheln, die manchmal nichts tun, und das ist genau die Sorte
 * toter Knopf, die man an dieser App nicht haben will.
 */
object ScreenOrder {

    fun ordered(config: LauncherConfig): List<Screen> {
        val echte = config.screens.filterNot { it.isFolder || it.id in config.swipeExcluded }
        if (config.swipeOrder.isEmpty()) return echte
        // Erst die ausdrücklich geordneten, dann der Rest in seiner natürlichen Folge.
        val nachOrdnung = config.swipeOrder.mapNotNull { id -> echte.firstOrNull { it.id == id } }
        return nachOrdnung + echte.filterNot { it.id in config.swipeOrder }
    }

    fun next(config: LauncherConfig, currentId: String): String? = step(config, currentId, +1)

    fun previous(config: LauncherConfig, currentId: String): String? = step(config, currentId, -1)

    private fun step(config: LauncherConfig, currentId: String, richtung: Int): String? {
        val reihe = ordered(config)
        if (reihe.size < 2) return null
        val jetzt = reihe.indexOfFirst { it.id == currentId }
        if (jetzt < 0) return null
        val naechster = ((jetzt + richtung) % reihe.size + reihe.size) % reihe.size
        return reihe[naechster].id
    }
}
