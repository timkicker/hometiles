package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.IconVisibility

/**
 * whether an icon has room on this tile. `PLAN.md` 4.2, only if there is room.
 *
 * not whether it fits somehow, [iconSizeDp] squeezes it small enough for that, but whether
 * it had to be squeezed: an unrecognisable icon still costs half the height.
 */
object IconRoom {

    /**
     * @param desiredDp the size the configured share gives
     * @param fittedDp  what is left of it beside the label
     */
    fun show(visibility: IconVisibility, desiredDp: Float, fittedDp: Float): Boolean =
        when (visibility) {
            IconVisibility.ALWAYS -> true
            IconVisibility.NEVER -> false
            IconVisibility.IF_ROOM -> fittedDp >= desiredDp
        }
}
