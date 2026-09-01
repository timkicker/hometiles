package org.biglau.tiles

import org.biglau.data.LauncherConfig

/**
 * Die Reihenfolge der Screens beim Wischen und fuer die Kacheln „naechster" und
 * „voriger". PLAN.md 4.1.
 *
 * `swipeOrder` stand von Anfang an im Modell und wurde von [ScreenOrder] gelesen - aber
 * von keiner Oberflaeche je geschrieben. Es blieb also immer leer, und die Reihenfolge war
 * immer die Anlegereihenfolge. Eine Zusage, die nichts tat.
 *
 * Bewegt wird eine Stelle nach oben oder unten, nicht gezogen. Ziehen setzt eine ruhige
 * Hand voraus - und die ist bei den Leuten, fuer die diese App gebaut ist, nicht
 * vorauszusetzen. Zweimal tippen bringt dieselbe Kachel zwei Stellen weiter.
 */
object SwipeChain {

    /** Die geltende Reihenfolge als ausdrueckliche Liste von Kennungen. */
    fun explicit(config: LauncherConfig): List<String> =
        ScreenOrder.ordered(config).map { it.id }

    fun moveUp(config: LauncherConfig, id: String): LauncherConfig = move(config, id, -1)

    fun moveDown(config: LauncherConfig, id: String): LauncherConfig = move(config, id, +1)

    /** Wie weit oben ein Screen steht, 0-basiert; -1, wenn er nicht in der Reihe ist. */
    fun position(config: LauncherConfig, id: String): Int = explicit(config).indexOf(id)

    private fun move(config: LauncherConfig, id: String, richtung: Int): LauncherConfig {
        val reihe = explicit(config).toMutableList()
        val jetzt = reihe.indexOf(id)
        val ziel = jetzt + richtung
        // Am Rand passiert nichts. Kein Umlauf: wer die oberste Kachel „nach oben" tippt,
        // erwartet nicht, dass sie unten herauskommt.
        if (jetzt < 0 || ziel !in reihe.indices) return config
        reihe[jetzt] = reihe[ziel]
        reihe[ziel] = id
        return config.copy(swipeOrder = reihe)
    }

    /**
     * Darf dieser Screen die Kette verlassen?
     *
     * Nur, wenn er danach noch anders zu erreichen ist - durch eine Sprungkachel oder
     * weil er der Startbildschirm ist. Sonst waere das Herausnehmen der schnellste Weg,
     * einen eingerichteten Screen unauffindbar zu machen: er stuende weiter in der
     * Konfiguration, und kein Weg fuehrte mehr hin.
     */
    fun mayLeave(config: LauncherConfig, id: String): Boolean =
        id == config.homeScreenId || hasJumpTile(config, id)

    fun exclude(config: LauncherConfig, id: String): LauncherConfig =
        if (!mayLeave(config, id)) config else config.copy(swipeExcluded = config.swipeExcluded + id)

    fun include(config: LauncherConfig, id: String): LauncherConfig =
        config.copy(swipeExcluded = config.swipeExcluded - id)

    /** Die Screens, die gerade nicht in der Kette liegen - in ihrer natuerlichen Folge. */
    fun excluded(config: LauncherConfig): List<org.biglau.data.Screen> =
        config.screens.filter { !it.isFolder && it.id in config.swipeExcluded }

    private fun hasJumpTile(config: LauncherConfig, id: String): Boolean = config.screens
        .flatMap { it.cells }
        .any { (it.button.action as? org.biglau.data.ButtonAction.GoToScreen)?.screenId == id }
}
