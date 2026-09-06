package dev.kicker.hometiles.phone

import androidx.annotation.StringRes
import dev.kicker.hometiles.core.system.R

/**
 * the word for a call's direction.
 *
 * this used to be a private function in the settings tree, where the types are switched on
 * and off. the call log needs the same words: there an arrow shows the direction, and an
 * arrow has no name to read out. keeping the list twice is the sure way to let it drift.
 *
 * it sits beside [CallDirection] rather than in `:app`, so `:app` gains no new edge for what
 * is only a word list.
 */
@StringRes
fun callDirectionLabel(direction: CallDirection): Int = when (direction) {
    CallDirection.INCOMING -> R.string.call_type_incoming
    CallDirection.OUTGOING -> R.string.call_type_outgoing
    CallDirection.MISSED -> R.string.call_type_missed
    CallDirection.REJECTED -> R.string.call_type_rejected
    CallDirection.BLOCKED -> R.string.call_type_blocked
    CallDirection.OTHER -> R.string.call_type_other
}

/**
 * the same for a single row: "missed" instead of "missed calls".
 *
 * the settings headings are plural category names, and hung onto a row they read wrong
 * ("Mark Helsi, answered calls"), while "everything else" said nothing at all. two phrasings
 * for two purposes, but in one file and on the same enum, so they cannot drift.
 */
@StringRes
fun callDirectionSpeech(direction: CallDirection): Int = when (direction) {
    CallDirection.INCOMING -> R.string.call_dir_incoming
    CallDirection.OUTGOING -> R.string.call_dir_outgoing
    CallDirection.MISSED -> R.string.call_dir_missed
    CallDirection.REJECTED -> R.string.call_dir_rejected
    CallDirection.BLOCKED -> R.string.call_dir_blocked
    CallDirection.OTHER -> R.string.call_dir_other
}
