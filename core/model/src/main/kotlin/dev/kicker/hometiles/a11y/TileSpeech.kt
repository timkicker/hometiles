package dev.kicker.hometiles.a11y

/**
 * what a tile says to the screen reader: "label (app). 3 new messages."
 *
 * badge, signal bars and charge are drawn, not written, so without this they are silent.
 */
object TileSpeech {

    /** empty parts drop out; the full stop makes the screen reader pause. */
    fun describe(label: String, state: String? = null, badge: String? = null): String =
        listOfNotNull(label, state, badge)
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotEmpty() }
            .joinToString(". ")
}
