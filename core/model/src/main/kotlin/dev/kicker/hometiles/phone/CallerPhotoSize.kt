package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.CallerPhoto

/**
 * how much height the caller's photo gets (`PLAN.md` 4.6).
 *
 * a fraction of the screen height, capped: what is left has to carry the name, the state and
 * the buttons. a photo that pushes the answer button off screen would be a fault on exactly
 * the screen where a fault means somebody cannot take a call.
 */
object CallerPhotoSize {

    /** height of one button row, the same 72 dp as in `ActionRow`. */
    const val ROW_DP = 72f

    /** the same `spacedBy(8.dp)` as in the layout. */
    const val ROW_GAP_DP = 8f

    /** name (up to two lines) and state above it. */
    const val HEADER_DP = 120f

    /**
     * empty space above the buttons, `PLAN.md` 4.6 ("against mis-taps with the ear").
     *
     * not a setting but a lower bound, and it belongs here because the photo is the only
     * thing that can give up room for it.
     */
    const val EAR_GAP_DP = 56f

    /** below this a strip shows no face. */
    const val MIN_PHOTO_DP = 64f

    /** the "call with ... in progress" line when a second call comes in. */
    const val NOTICE_DP = 44f

    /**
     * what must fit below the photo, for [buttons] rows.
     *
     * a fixed number here was the fault: the button count swings between two while ringing
     * and five during a call, and with a half-height photo only two of five stayed on
     * screen. speaker, hold and keypad were unreachable, and this screen does not scroll.
     */
    fun reservedDp(buttons: Int, notice: Boolean = false): Float =
        HEADER_DP + buttons * ROW_DP + (buttons + 1) * ROW_GAP_DP + EAR_GAP_DP +
            if (notice) NOTICE_DP else 0f

    fun fractionOf(size: CallerPhoto): Float = when (size) {
        CallerPhoto.OFF -> 0f
        CallerPhoto.SMALL -> 0.18f
        CallerPhoto.HALF -> 0.5f
        CallerPhoto.FULL -> 1f
    }

    enum class Image { NONE, PHOTO, INITIALS }

    /**
     * with a contact but no photo, half of the most important screen used to stay black.
     * initials fill that place everywhere else in the app, and a coloured field with "AB" is
     * recognised faster on three inches than a name is read.
     *
     * an unknown number stays empty: "+43" yields no character that means anything.
     */
    fun imageFor(heightDp: Float, photoUri: String?, name: String?): Image = when {
        heightDp <= 0f -> Image.NONE
        photoUri != null -> Image.PHOTO
        !name.isNullOrBlank() -> Image.INITIALS
        else -> Image.NONE
    }

    /**
     * null height means: show no photo.
     *
     * in doubt the photo yields, not the button. on three inches that means no room for a
     * photo during a call, and that is the right order: a picture covering the speaker
     * button helps nobody.
     */
    fun heightDp(
        size: CallerPhoto,
        availableDp: Float,
        buttons: Int,
        notice: Boolean = false,
    ): Float {
        if (size == CallerPhoto.OFF) return 0f
        val room = (availableDp - reservedDp(buttons, notice)).coerceAtLeast(0f)
        if (room < MIN_PHOTO_DP) return 0f
        return (availableDp * fractionOf(size)).coerceAtMost(room)
    }
}
