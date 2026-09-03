package org.biglau.a11y

import org.biglau.data.Accessibility
import org.biglau.data.PressMode

enum class LongPressAction { EDIT, SPEAK, POPUP, ACTIVATE, SECOND_ACTION, NOTHING }

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

    fun decide(
        accessibility: Accessibility,
        editMode: Boolean,
        pressMode: PressMode = PressMode.SHORT,
        /** Hat diese Kachel eine eigene Zweitbelegung? (`PLAN.md` 4.3, Zeile 485) */
        hasSecondAction: Boolean = false,
    ): List<LongPressAction> = when {
        editMode -> listOf(LongPressAction.EDIT)
        // Eine Zweitbelegung ist eine Entscheidung fuer genau diese Kachel und geht
        // deshalb den allgemeinen Vorgaben vor. Wer sie setzt, will sie auch ausloesen.
        hasSecondAction -> listOf(LongPressAction.SECOND_ACTION)
        // Wer den Langdruck zum Ausloesen gewaehlt hat, bekommt genau das. Vorlesen und
        // Editor muessen dann anderswo hin - beides geht ueber die Einstellungen.
        pressMode == PressMode.LONG -> listOf(LongPressAction.ACTIVATE)
        accessibility.speakOnLongPress && accessibility.popupOnLongPress ->
            listOf(LongPressAction.SPEAK, LongPressAction.POPUP)
        accessibility.speakOnLongPress -> listOf(LongPressAction.SPEAK)
        accessibility.popupOnLongPress -> listOf(LongPressAction.POPUP)
        else -> listOf(LongPressAction.EDIT)
    }

    /** Erreicht der Nutzer den Editor ueberhaupt noch per Langdruck? */
    fun editorReachableByLongPress(
        accessibility: Accessibility,
        editMode: Boolean,
        pressMode: PressMode = PressMode.SHORT,
        hasSecondAction: Boolean = false,
    ): Boolean = LongPressAction.EDIT in decide(accessibility, editMode, pressMode, hasSecondAction)

    /**
     * Muss die Oberflaeche einen anderen Weg zum Editor anbieten?
     * Genau dann, wenn der Langdruck ihn nicht mehr oeffnet - sonst waere die Belegung
     * unerreichbar, sobald jemand das Vorlesen einschaltet.
     */
    fun needsEditModeEntry(
        accessibility: Accessibility,
        pressMode: PressMode = PressMode.SHORT,
    ): Boolean =
        !editorReachableByLongPress(accessibility, editMode = false, pressMode = pressMode)
}
