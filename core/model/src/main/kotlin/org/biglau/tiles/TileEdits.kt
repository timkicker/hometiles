package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction

/** editing steps for a tile as pure functions: this is where it is decided what a reassignment loses. */
object TileEdits {

    /** colour from the grid position, so a new screen looks sorted at once. */
    fun autoColorIndex(x: Int, y: Int, cols: Int, paletteSize: Int): Int {
        require(cols > 0) { "a grid needs at least one column" }
        require(paletteSize > 0) { "the palette must not be empty" }
        return (y * cols + x).mod(paletteSize)
    }

    /**
     * a hand-written label survives a new action of the same kind, or every reassignment
     * would be a silent loss. across kinds it goes, because "granny" on a camera tile is
     * worse than no label.
     */
    fun withAction(button: Button, action: ButtonAction): Button {
        val keepLabel = button.label != null && sameKind(button.action, action)
        return button.copy(action = action, label = if (keepLabel) button.label else null)
    }

    /** empty input means "label automatically", not "empty label". */
    fun withLabel(button: Button, label: String?): Button =
        button.copy(label = label?.trim()?.takeIf { it.isNotEmpty() })

    /** null means "from the position". clears a free hue, or there would be two answers. */
    fun withColorIndex(button: Button, index: Int?): Button =
        button.copy(colorIndex = index ?: -1, colorHue = null)

    /** clears the palette choice for the same reason. */
    fun withColorHue(button: Button, hue: Float): Button =
        button.copy(colorHue = hue, colorIndex = -1)

    /** a blank name counts as none, or a slip would leave a tile without an icon and no way back. */
    fun withIcon(button: Button, name: String?): Button =
        button.copy(iconName = name?.takeIf { it.isNotBlank() })

    /**
     * is there anything to clear on this tile?
     *
     * compared against a fresh button, not just against the action: an empty tile may carry
     * a label of its own, and that *is* something to clear.
     */
    fun clearable(button: Button): Boolean = button != Button()

    fun withLongPress(button: Button, action: ButtonAction?): Button =
        button.copy(longPress = action?.takeIf { it != ButtonAction.None })

    private fun sameKind(a: ButtonAction, b: ButtonAction): Boolean = when (a) {
        is ButtonAction.App -> b is ButtonAction.App
        is ButtonAction.Contact -> b is ButtonAction.Contact
        is ButtonAction.GoToScreen -> b is ButtonAction.GoToScreen
        is ButtonAction.Folder -> b is ButtonAction.Folder
        is ButtonAction.Link -> b is ButtonAction.Link
        is ButtonAction.Shortcut -> b is ButtonAction.Shortcut
        is ButtonAction.Widget -> b is ButtonAction.Widget
        is ButtonAction.Action -> b is ButtonAction.Action
        ButtonAction.None -> b == ButtonAction.None
    }
}
