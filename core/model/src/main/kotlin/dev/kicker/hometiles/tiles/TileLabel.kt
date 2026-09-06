package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.web.LinkTarget

/**
 * what a tile says when nobody has labelled it (`PLAN.md` 4.4).
 *
 * derived every time rather than written into the config: rename an app or a folder and the
 * tile follows. a label copied in once would stay put and be wrong from then on.
 *
 * this derivation used to exist twice, for the home screen and for the editor, and the two
 * had already drifted: one named the folder, the other just said "folder".
 */
object TileLabel {

    /** words from the resources, handed in so the derivation stays free of android. */
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
