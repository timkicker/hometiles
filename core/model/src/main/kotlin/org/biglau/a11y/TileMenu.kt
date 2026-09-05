package org.biglau.a11y

/** what stands in the menu-key list. `PLAN.md` 10.3.4. */
enum class MenuItem { SECOND_ACTION, EDIT, SPEAK, SHOW_LARGE }

/**
 * the small list the left softkey opens for the focused tile.
 *
 * a long press can only do one of three things, and which one is a setting
 * ([LongPress.decide]). that works with a finger, because the setting can be found and
 * changed. with keys it does not: whoever turns on reading-aloud never reaches the editor
 * through a long press again, and the setting that would undo it sits behind that very
 * editor.
 *
 * so the list shows **all three**, whatever is set. more key presses than a long press, and
 * visible in return; a key phone has nothing that announces a hidden gesture.
 */
object TileMenu {

    /** a second action belongs to this one tile and comes before the general entries. */
    fun items(hasSecondAction: Boolean = false): List<MenuItem> = buildList {
        if (hasSecondAction) add(MenuItem.SECOND_ACTION)
        add(MenuItem.EDIT)
        add(MenuItem.SPEAK)
        add(MenuItem.SHOW_LARGE)
    }
}
