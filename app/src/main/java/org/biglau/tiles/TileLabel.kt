package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.web.LinkTarget

/**
 * Was auf einer Kachel steht, wenn niemand sie beschriftet hat.
 *
 * PLAN.md 4.4 verlangt automatische Beschriftungen fuer neu angelegte Kacheln. Sie werden
 * nicht in die Konfiguration geschrieben, sondern jedes Mal abgeleitet: benennt der Nutzer
 * eine App um oder einen Ordner, wandert die Kachel mit. Eine einmal hineinkopierte
 * Beschriftung bliebe stehen und waere ab dann falsch.
 *
 * Frueher gab es diese Ableitung zweimal - einmal fuer den Startbildschirm, einmal fuer
 * den Editor - und sie waren bereits auseinander: der Startbildschirm nannte den Ordner
 * beim Namen, der Editor sagte nur "Ordner". Wer zwei Ordner hat, konnte dort nicht mehr
 * erkennen, welcher gemeint war.
 */
object TileLabel {

    /**
     * Die Woerter, die aus den Ressourcen kommen. Von aussen hereingereicht, damit die
     * Ableitung selbst ohne Android auskommt und pruefbar bleibt.
     */
    data class Words(
        val emptyTile: String,
        val folder: String,
        val nextScreen: String,
        val widget: String,
    )

    fun of(
        button: Button,
        words: Words,
        screenName: (String) -> String?,
        appLabel: (ButtonAction.App) -> String?,
        builtinLabel: (Builtin) -> String,
    ): String {
        button.label?.let { return it }
        return when (val action = button.action) {
            is ButtonAction.Action -> builtinLabel(action.builtin)
            is ButtonAction.App -> appLabel(action) ?: action.packageName
            is ButtonAction.Contact -> action.name
            is ButtonAction.Shortcut -> action.label
            is ButtonAction.Widget -> action.label.ifBlank { words.widget }
            is ButtonAction.GoToScreen -> screenName(action.screenId) ?: words.nextScreen
            is ButtonAction.Folder -> screenName(action.screenId) ?: words.folder
            is ButtonAction.Link -> LinkTarget.labelFor(action.url)
            ButtonAction.None -> words.emptyTile
        }
    }
}
