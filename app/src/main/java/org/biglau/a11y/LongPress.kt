package org.biglau.a11y

import org.biglau.data.Accessibility

enum class LongPressAction { EDIT, SPEAK, POPUP, NOTHING }

/**
 * Was ein langer Druck auf eine Kachel tut.
 *
 * Hier kollidieren zwei Wuensche aus dem Plan: der Kachel-Editor haengt am Langdruck, und
 * die Barrierefreiheit will ihn zum Vorlesen. Beides gleichzeitig geht nicht, also entscheidet
 * eine Regel statt einer Ueberlagerung:
 *
 * - Im Bearbeitungsmodus fuehrt jeder Druck zum Editor. Wer bearbeitet, will bearbeiten.
 * - Sonst gewinnt die Barrierefreiheit, wenn sie eingeschaltet ist. Wer sich Kacheln vorlesen
 *   laesst, weil er sie nicht liest, darf nicht versehentlich im Editor landen.
 * - Ist beides eingeschaltet, wird gesprochen *und* angezeigt - das widerspricht sich nicht.
 * - Sonst der Editor, wie bisher.
 */
object LongPress {

    fun decide(accessibility: Accessibility, editMode: Boolean): List<LongPressAction> = when {
        editMode -> listOf(LongPressAction.EDIT)
        accessibility.speakOnLongPress && accessibility.popupOnLongPress ->
            listOf(LongPressAction.SPEAK, LongPressAction.POPUP)
        accessibility.speakOnLongPress -> listOf(LongPressAction.SPEAK)
        accessibility.popupOnLongPress -> listOf(LongPressAction.POPUP)
        else -> listOf(LongPressAction.EDIT)
    }

    /** Erreicht der Nutzer den Editor ueberhaupt noch per Langdruck? */
    fun editorReachableByLongPress(accessibility: Accessibility, editMode: Boolean): Boolean =
        LongPressAction.EDIT in decide(accessibility, editMode)

    /**
     * Muss die Oberflaeche einen anderen Weg zum Editor anbieten?
     * Genau dann, wenn der Langdruck ihn nicht mehr oeffnet - sonst waere die Belegung
     * unerreichbar, sobald jemand das Vorlesen einschaltet.
     */
    fun needsEditModeEntry(accessibility: Accessibility): Boolean =
        !editorReachableByLongPress(accessibility, editMode = false)
}
