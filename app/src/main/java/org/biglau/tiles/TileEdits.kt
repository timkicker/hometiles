package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction

/**
 * Die Bearbeitungsschritte einer Kachel als reine Funktionen. Bewusst getrennt von der
 * Oberflaeche: hier entscheidet sich, was der Nutzer beim Belegen verliert und was nicht.
 */
object TileEdits {

    /** Farbe automatisch aus der Rasterposition, damit ein neuer Screen sofort sortiert wirkt. */
    fun autoColorIndex(x: Int, y: Int, cols: Int, paletteSize: Int): Int {
        require(cols > 0) { "Raster braucht mindestens eine Spalte" }
        require(paletteSize > 0) { "Palette darf nicht leer sein" }
        return (y * cols + x).mod(paletteSize)
    }

    /**
     * Neue Aktion setzen. Eine selbst vergebene Beschriftung bleibt erhalten, wenn sie zur
     * neuen Aktion noch passt - sonst waere jede Umbelegung ein stiller Datenverlust.
     * Bei einem Wechsel der Aktionsart faellt sie weg, weil "Oma" auf einer Kamera-Kachel
     * schlimmer ist als gar keine Beschriftung.
     */
    fun withAction(button: Button, action: ButtonAction): Button {
        val keepLabel = button.label != null && sameKind(button.action, action)
        return button.copy(action = action, label = if (keepLabel) button.label else null)
    }

    /** Leere Eingabe heisst "automatisch beschriften", nicht "leere Beschriftung". */
    fun withLabel(button: Button, label: String?): Button =
        button.copy(label = label?.trim()?.takeIf { it.isNotEmpty() })

    /** index null heisst: Farbe wieder automatisch aus der Position ableiten. */
    fun withColorIndex(button: Button, index: Int?): Button =
        button.copy(colorIndex = index ?: -1, customColor = null)

    fun withCustomColor(button: Button, argb: Long): Button =
        button.copy(customColor = argb)

    /** Kachel leeren heisst wirklich leeren - auch Beschriftung und Farbe. */
    fun cleared(): Button = Button()

    fun withBlink(button: Button, blink: Boolean): Button = button.copy(blink = blink)

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
