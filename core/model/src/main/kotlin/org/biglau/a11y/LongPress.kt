package org.biglau.a11y

import org.biglau.data.Accessibility
import org.biglau.data.PressMode

enum class LongPressAction { EDIT, SPEAK, POPUP, ACTIVATE, SECOND_ACTION, NOTHING }

/**
 * what a long press on a tile does.
 *
 * two wishes from the plan collide here: the tile editor hangs off the long press, and
 * accessibility wants it for reading aloud. a rule decides instead of a pile-up:
 *
 * - in edit mode every press leads to the editor. whoever edits wants to edit.
 * - otherwise accessibility wins when it is on: whoever has tiles read out because they
 *   cannot read them must not land in the editor by accident.
 * - with both on, it speaks *and* shows; those do not contradict each other.
 * - otherwise the editor, as before.
 */
object LongPress {

    fun decide(
        accessibility: Accessibility,
        editMode: Boolean,
        pressMode: PressMode = PressMode.SHORT,
        /** does this tile carry a second action of its own? (`PLAN.md` 4.3) */
        hasSecondAction: Boolean = false,
    ): List<LongPressAction> = when {
        editMode -> listOf(LongPressAction.EDIT)
        // a second action is a decision about this one tile and comes before the general
        // settings. whoever sets it wants to trigger it.
        hasSecondAction -> listOf(LongPressAction.SECOND_ACTION)
        // whoever chose the long press for triggering gets exactly that; speaking and the
        // editor then go through the settings.
        pressMode == PressMode.LONG -> listOf(LongPressAction.ACTIVATE)
        accessibility.speakOnLongPress && accessibility.popupOnLongPress ->
            listOf(LongPressAction.SPEAK, LongPressAction.POPUP)
        accessibility.speakOnLongPress -> listOf(LongPressAction.SPEAK)
        accessibility.popupOnLongPress -> listOf(LongPressAction.POPUP)
        else -> listOf(LongPressAction.EDIT)
    }

    fun editorReachableByLongPress(
        accessibility: Accessibility,
        editMode: Boolean,
        pressMode: PressMode = PressMode.SHORT,
        hasSecondAction: Boolean = false,
    ): Boolean = LongPressAction.EDIT in decide(accessibility, editMode, pressMode, hasSecondAction)

    /** a second way into the editor is needed exactly when the long press no longer opens it. */
    fun needsEditModeEntry(
        accessibility: Accessibility,
        pressMode: PressMode = PressMode.SHORT,
    ): Boolean =
        !editorReachableByLongPress(accessibility, editMode = false, pressMode = pressMode)
}
